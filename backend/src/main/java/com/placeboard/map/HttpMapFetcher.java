package com.placeboard.map;

import com.placeboard.common.MapProviderConnectionException;
import com.placeboard.place.UnsupportedMapUrlException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Component
public class HttpMapFetcher implements MapHttpFetcher {
    private static final int MAX_REDIRECTS = 5;
    private static final Set<Integer> REDIRECT_STATUS_CODES = Set.of(301, 302, 303, 307, 308);
    private final HttpClient httpClient;

    public HttpMapFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public MapHttpResponse get(URI uri, Map<String, String> headers, Predicate<URI> allowedUri) {
        URI current = uri;
        for (int count = 0; count <= MAX_REDIRECTS; count++) {
            if (!"https".equalsIgnoreCase(current.getScheme()) || !allowedUri.test(current)) {
                throw new UnsupportedMapUrlException("허용되지 않은 지도 주소로 이동했습니다.");
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .GET();
            headers.forEach(builder::header);
            try {
                HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
                if (!REDIRECT_STATUS_CODES.contains(response.statusCode())) {
                    return new MapHttpResponse(response.statusCode(), current,
                            new String(response.body(), StandardCharsets.UTF_8));
                }
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new MapProviderConnectionException("리다이렉트 주소가 없습니다."));
                current = current.resolve(location);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new MapProviderConnectionException("지도 서비스 연결이 중단되었습니다.", exception);
            } catch (IOException exception) {
                throw new MapProviderConnectionException("지도 서비스에 연결할 수 없습니다.", exception);
            }
        }
        throw new MapProviderConnectionException("지도 주소 리다이렉트 횟수를 초과했습니다.");
    }
}
