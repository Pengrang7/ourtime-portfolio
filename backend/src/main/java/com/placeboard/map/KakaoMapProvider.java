package com.placeboard.map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import com.placeboard.common.MapProviderConnectionException;
import com.placeboard.place.PlacePreviewResponse;
import com.placeboard.place.UnsupportedMapUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.placeboard.map.NaverMapProvider.decimal;
import static com.placeboard.map.NaverMapProvider.text;

@Component
public class KakaoMapProvider implements MapProvider {
    private static final Set<String> SHORT_HOSTS = Set.of("kko.to", "kko.kakao.com");
    private static final Set<String> HOSTS = Set.of("kko.to", "kko.kakao.com", "place.map.kakao.com",
            "map.kakao.com", "m.map.kakao.com", "place-api.map.kakao.com");
    private static final Pattern PLACE_PATH = Pattern.compile("^/(?:m/)?(\\d+)(?:/|$)");
    private static final Pattern LINK_MAP_PATH = Pattern.compile("^/link/(?:map|to|roadview)/(\\d+)(?:/|$)");
    private static final Pattern ITEM_ID_QUERY = Pattern.compile("(?:^|&)itemId=(\\d+)");

    private final MapHttpFetcher fetcher;
    private final ObjectMapper objectMapper;

    public KakaoMapProvider(MapHttpFetcher fetcher, ObjectMapper objectMapper) {
        this.fetcher = fetcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String url) {
        URI uri = Hosts.parseHttps(url);
        return uri != null && isKakao(uri);
    }

    @Override
    public PlacePreviewResponse parse(String url) {
        URI uri = Hosts.parseHttps(url);
        if (uri == null) throw new UnsupportedMapUrlException("올바른 URL 형식이 아닙니다.");
        URI resolved = SHORT_HOSTS.contains(uri.getHost().toLowerCase())
                ? fetcher.get(uri, Map.of("User-Agent", MapHttpFetcher.DESKTOP_USER_AGENT), this::isKakao).finalUri()
                : uri;
        String placeId = extractPlaceId(resolved);
        return fetchDetail(placeId, url);
    }

    static String extractPlaceId(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        String host = uri.getHost().toLowerCase();
        Matcher matcher = LINK_MAP_PATH.matcher(path);
        if (matcher.find()) return matcher.group(1);
        if (host.equals("place.map.kakao.com")) {
            matcher = PLACE_PATH.matcher(path);
            if (matcher.find()) return matcher.group(1);
        }
        if (uri.getRawQuery() != null) {
            matcher = ITEM_ID_QUERY.matcher(uri.getRawQuery());
            if (matcher.find()) return matcher.group(1);
        }
        throw new UnsupportedMapUrlException("카카오 장소 ID를 찾을 수 없습니다.");
    }

    private PlacePreviewResponse fetchDetail(String placeId, String originalUrl) {
        MapHttpFetcher.MapHttpResponse response = fetcher.get(
                URI.create("https://place-api.map.kakao.com/places/panel3/" + placeId),
                Map.of("User-Agent", MapHttpFetcher.DESKTOP_USER_AGENT,
                        "Accept", "application/json",
                        "pf", "web",
                        "Referer", "https://place.map.kakao.com/",
                        "Origin", "https://place.map.kakao.com"),
                this::isKakao);
        if (!response.ok()) {
            throw new MapProviderConnectionException("카카오 장소 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        JsonNode summary;
        try {
            summary = objectMapper.readTree(response.body()).path("summary");
        } catch (JacksonException exception) {
            throw new MapProviderConnectionException("카카오 장소 정보 형식을 해석하지 못했습니다.", exception);
        }
        JsonNode address = summary.path("address");
        String road = text(address, "road");
        String jibun = text(address, "jibun");
        if (jibun != null && road != null) {
            // jibun은 "진천동 92-9"처럼 시·군·구가 빠져 있으므로 도로명 앞부분을 붙여 지역을 해석한다.
            String[] roadTokens = road.split("\\s+");
            if (roadTokens.length >= 2) jibun = roadTokens[0] + " " + roadTokens[1] + " " + jibun;
        }
        String phone = null;
        for (JsonNode number : summary.path("phone_numbers")) {
            if (!"FAX".equals(text(number, "type"))) {
                phone = text(number, "tel");
                break;
            }
        }
        JsonNode category = summary.path("category");
        String categoryName = text(category, "name3") != null ? text(category, "name3") : text(category, "name");
        return PlacePreviewResponse.of(MapProviderType.KAKAO, placeId, originalUrl, text(summary, "name"),
                road, jibun, decimal(summary.path("point").path("lat")), decimal(summary.path("point").path("lon")),
                categoryName, phone);
    }

    private boolean isKakao(URI uri) {
        return Hosts.matches(uri, HOSTS, Set.of());
    }
}
