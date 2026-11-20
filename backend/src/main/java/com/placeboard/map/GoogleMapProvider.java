package com.placeboard.map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import com.placeboard.common.MapProviderConnectionException;
import com.placeboard.place.GooglePlaceDetails;
import com.placeboard.place.NaverBusinessHour;
import com.placeboard.place.PlacePreviewResponse;
import com.placeboard.place.UnsupportedMapUrlException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.placeboard.map.NaverMapProvider.decimal;

@Component
public class GoogleMapProvider implements MapProvider {
    private static final Set<String> SHORT_HOSTS = Set.of("maps.app.goo.gl", "goo.gl");
    private static final Set<String> GOOGLE_HOSTS = Set.of("google.com", "www.google.com", "maps.google.com",
            "google.co.kr", "www.google.co.kr", "maps.google.co.kr");
    private static final Pattern PREVIEW_LINK = Pattern.compile("href=\"(/maps/preview/place\\?[^\"]+)\"");
    private static final Pattern PLACE_NAME_PATH = Pattern.compile("/maps/place/([^/]+)");
    private static final Pattern DATA_COORDINATE = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");
    private static final Pattern AT_COORDINATE = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern FEATURE_ID = Pattern.compile("(0x[0-9a-f]+:0x[0-9a-f]+)");

    private final MapHttpFetcher fetcher;
    private final ObjectMapper objectMapper;

    public GoogleMapProvider(MapHttpFetcher fetcher, ObjectMapper objectMapper) {
        this.fetcher = fetcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String url) {
        URI uri = Hosts.parseHttps(url);
        if (uri == null) return false;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (host.equals("maps.app.goo.gl")) return true;
        if (host.equals("goo.gl")) return path.startsWith("/maps");
        return isGoogle(uri) && (host.startsWith("maps.") || path.startsWith("/maps"));
    }

    @Override
    public PlacePreviewResponse parse(String url) {
        URI uri = Hosts.parseHttps(url);
        if (uri == null) throw new UnsupportedMapUrlException("올바른 URL 형식이 아닙니다.");
        Map<String, String> headers = Map.of("User-Agent", MapHttpFetcher.DESKTOP_USER_AGENT, "Accept", "text/html");
        MapHttpFetcher.MapHttpResponse page = fetcher.get(uri, headers, this::isAllowedHop);
        if (!page.ok()) {
            throw new MapProviderConnectionException("구글 지도 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (!isGoogle(page.finalUri())) {
            throw new UnsupportedMapUrlException("구글 지도 장소 링크가 아닙니다.");
        }

        String decodedUrl = URLDecoder.decode(page.finalUri().toString(), StandardCharsets.UTF_8);
        String featureId = findGroup(FEATURE_ID, decodedUrl, 1);
        // 장소 ID가 URL에 있으면 해당 장소를 직접 조회하고, 없으면 페이지에 포함된 미리보기 요청을 사용한다.
        JsonNode place = featureId != null
                ? fetchPreview("/maps/preview/place?authuser=0&hl=ko&gl=kr&pb=!1m1!1s" + featureId)
                : fetchPreview(findGroup(PREVIEW_LINK, page.body(), 1));
        String name = textAt(place, 11);
        String address = textAt(place, 39) != null ? textAt(place, 39) : joinAddress(place.path(2));
        BigDecimal latitude = decimal(place.path(9).path(2));
        BigDecimal longitude = decimal(place.path(9).path(3));
        if (textAt(place, 10) != null) featureId = textAt(place, 10);
        String category = textAt(place.path(13), 0);
        String phone = textAt(place.path(178).path(0), 0);

        if (name == null) name = findGroup(PLACE_NAME_PATH, decodedUrl, 1);
        if (name != null) name = name.replace('+', ' ');
        if (latitude == null || longitude == null) {
            Matcher matcher = DATA_COORDINATE.matcher(decodedUrl);
            boolean found = matcher.find();
            if (!found) {
                matcher = AT_COORDINATE.matcher(decodedUrl);
                found = matcher.find();
            }
            if (found) {
                latitude = new BigDecimal(matcher.group(1));
                longitude = new BigDecimal(matcher.group(2));
            }
        }
        PlacePreviewResponse preview = PlacePreviewResponse.of(MapProviderType.GOOGLE, featureId, url, name,
                address, null, latitude, longitude, category, phone);
        return preview.withGoogleDetails(new GooglePlaceDetails(
                textAt(place, 78), decimal(place.path(4).path(7)), integerAt(place.path(4), 8),
                textAt(place.path(32).path(1), 1), textAt(place.path(203).path(1).path(8), 0),
                textAt(place.path(203).path(1).path(4), 0), businessHours(place), websiteUrl(place)));
    }

    private JsonNode fetchPreview(String link) {
        if (link == null) return objectMapper.createArrayNode();
        URI previewUri = URI.create("https://www.google.com" + link.replace("&amp;", "&"));
        MapHttpFetcher.MapHttpResponse response = fetcher.get(previewUri,
                Map.of("User-Agent", MapHttpFetcher.DESKTOP_USER_AGENT), this::isGoogle);
        if (!response.ok()) return objectMapper.createArrayNode();
        String body = response.body();
        int jsonStart = body.indexOf('\n');
        try {
            return objectMapper.readTree(jsonStart < 0 ? body : body.substring(jsonStart + 1)).path(6);
        } catch (JacksonException exception) {
            return objectMapper.createArrayNode();
        }
    }

    private String joinAddress(JsonNode parts) {
        if (!parts.isArray() || parts.isEmpty()) return null;
        StringBuilder builder = new StringBuilder();
        parts.forEach(part -> {
            if (part.isTextual()) builder.append(builder.isEmpty() ? "" : " ").append(part.asText());
        });
        return builder.isEmpty() ? null : builder.toString();
    }

    private static String textAt(JsonNode node, int index) {
        JsonNode value = node.path(index);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static Integer integerAt(JsonNode node, int index) {
        JsonNode value = node.path(index);
        return value.isIntegralNumber() ? value.intValue() : null;
    }

    private static List<NaverBusinessHour> businessHours(JsonNode place) {
        JsonNode hours = place.path(203).path(0);
        if (!hours.isArray()) return List.of();
        List<NaverBusinessHour> result = new ArrayList<>();
        hours.forEach(item -> {
            String day = textAt(item, 0);
            String value = textAt(item.path(3).path(0), 0);
            String description = textAt(item.path(6), 0);
            if (day != null && value != null) result.add(new NaverBusinessHour(day, value, description));
        });
        return List.copyOf(result);
    }

    private static String websiteUrl(JsonNode place) {
        String link = textAt(place.path(7), 0);
        if (link == null) return null;
        if (link.startsWith("/url?q=")) {
            String encoded = link.substring(7);
            int separator = encoded.indexOf('&');
            if (separator >= 0) encoded = encoded.substring(0, separator);
            link = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        }
        return Hosts.parseHttps(link) == null ? null : link;
    }

    private static String findGroup(Pattern pattern, String value, int group) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(group) : null;
    }

    private boolean isAllowedHop(URI uri) {
        return SHORT_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT)) || isGoogle(uri);
    }

    private boolean isGoogle(URI uri) {
        return Hosts.matches(uri, GOOGLE_HOSTS, Set.of());
    }
}
