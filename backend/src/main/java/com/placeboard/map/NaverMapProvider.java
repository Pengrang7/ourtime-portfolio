package com.placeboard.map;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.placeboard.common.MapProviderConnectionException;
import com.placeboard.place.NaverBusinessHour;
import com.placeboard.place.NaverMenuItem;
import com.placeboard.place.NaverPlaceDetails;
import com.placeboard.place.PlacePreviewResponse;
import com.placeboard.place.PlaceUnavailableException;
import com.placeboard.place.UnsupportedMapUrlException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NaverMapProvider implements MapProvider {
    private static final Set<String> HOSTS = Set.of("naver.me", "map.naver.com", "place.naver.com");
    private static final Set<String> HOST_SUFFIXES = Set.of("map.naver.com", "place.naver.com");
    private static final Pattern PLACE_ID_PATTERN = Pattern.compile("/[a-z]+/(\\d{5,})(?:/|$)");
    private static final Pattern PIN_ID_QUERY = Pattern.compile("(?:^|&)(?:pinId|id)=(\\d{5,})(?:&|$)");
    private static final Pattern OG_TITLE = Pattern.compile("property=\"og:title\" content=\"([^\"]+)\"");
    private static final String APOLLO_STATE = "window.__APOLLO_STATE__";
    private static final Map<String, String> PLACE_HEADERS = Map.of(
            "User-Agent", MapHttpFetcher.MOBILE_USER_AGENT, "Accept", "text/html");

    private final MapHttpFetcher fetcher;
    private final ObjectMapper objectMapper;

    public NaverMapProvider(MapHttpFetcher fetcher, ObjectMapper objectMapper) {
        this.fetcher = fetcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String url) {
        URI uri = Hosts.parseHttps(url);
        return uri != null && isNaver(uri);
    }

    @Override
    public PlacePreviewResponse parse(String url) {
        URI uri = Hosts.parseHttps(url);
        if (uri == null) throw new UnsupportedMapUrlException("올바른 URL 형식이 아닙니다.");
        URI resolved = "naver.me".equalsIgnoreCase(uri.getHost())
                ? fetcher.get(uri, Map.of("User-Agent", MapHttpFetcher.DESKTOP_USER_AGENT), this::isNaver).finalUri()
                : uri;
        String placeId = extractPlaceId(resolved);
        return fetchDetail(placeId, url);
    }

    static String extractPlaceId(URI uri) {
        Matcher matcher = PLACE_ID_PATTERN.matcher(uri.getPath() == null ? "" : uri.getPath());
        if (matcher.find()) return matcher.group(1);
        // 모바일 앱 링크 형식: m.map.naver.com/appLink.naver?pinId=1610498965
        matcher = PIN_ID_QUERY.matcher(uri.getRawQuery() == null ? "" : uri.getRawQuery());
        if (matcher.find()) return matcher.group(1);
        throw new UnsupportedMapUrlException("네이버 장소 ID를 찾을 수 없습니다.");
    }

    private PlacePreviewResponse fetchDetail(String placeId, String originalUrl) {
        MapHttpFetcher.MapHttpResponse response = fetcher.get(
                URI.create("https://m.place.naver.com/place/" + placeId + "/home"),
                PLACE_HEADERS,
                this::isNaver);
        if (response.status() == 404 || response.status() == 410) {
            throw new PlaceUnavailableException("네이버에서 더 이상 장소 정보를 확인할 수 없습니다.");
        }
        if (!response.ok()) {
            throw new MapProviderConnectionException("네이버 장소 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        JsonNode state = readApolloState(response.body());
        JsonNode base = state.path("PlaceDetailBase:" + placeId);
        String name = text(base, "name");
        if (name == null) name = ogTitle(response.body());
        JsonNode coordinate = base.path("coordinate");
        String phone = text(base, "phone") != null ? text(base, "phone") : text(base, "virtualPhone");
        PlacePreviewResponse preview = PlacePreviewResponse.of(MapProviderType.NAVER, placeId, originalUrl, name,
                text(base, "roadAddress"), text(base, "address"),
                decimal(coordinate.path("y")), decimal(coordinate.path("x")),
                text(base, "category"), phone);
        List<NaverMenuItem> menus = menuItems(state);
        JsonNode placeMenus = state.findValue("placeMenus");
        Integer menuCount = placeMenus == null ? null : integer(placeMenus, "menuCount");
        if (menuCount != null && menuCount > menus.size()) {
            menus = fetchFullMenus(placeId, menus);
        }
        List<NaverBusinessHour> businessHours = businessHours(state);
        JsonNode businessStatus = businessStatus(state);
        return preview.withNaverDetails(new NaverPlaceDetails(
                imageUrls(state, placeId, menus), integer(base, "visitorReviewsTotal"),
                integer(base, "cafeBlogReviewsTotal"), decimal(base.path("visitorReviewsScore")),
                textList(base.path("microReviews")), textList(base.path("conveniences")),
                text(businessStatus, "status"), text(businessStatus, "description"),
                businessHours, menus, menuImageUrls(state)));
    }

    private JsonNode readApolloState(String html) {
        int start = html.indexOf(APOLLO_STATE);
        if (start < 0) return objectMapper.createObjectNode();
        int assign = html.indexOf('=', start);
        try (JsonParser parser = objectMapper.createParser(html.substring(assign + 1))) {
            JsonNode node = parser.readValueAsTree();
            return node == null ? objectMapper.createObjectNode() : node;
        } catch (JacksonException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String ogTitle(String html) {
        Matcher matcher = OG_TITLE.matcher(html);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("&amp;", "&").replaceFirst("\\s*:\\s*네이버$", "");
    }

    private boolean isNaver(URI uri) {
        return Hosts.matches(uri, HOSTS, HOST_SUFFIXES);
    }

    static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    static BigDecimal decimal(JsonNode node) {
        if (node.isNumber()) return node.decimalValue();
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isIntegralNumber() ? value.intValue() : null;
    }

    private static List<String> textList(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) values.add(value.asText().trim());
        });
        return List.copyOf(values);
    }

    private static List<String> imageUrls(JsonNode state, String placeId, List<NaverMenuItem> menus) {
        Set<String> urls = new LinkedHashSet<>();
        state.properties().forEach(entry -> {
            if (urls.size() >= 30 || !entry.getKey().startsWith("PlaceDetailTopPhotoItem:" + placeId + "_")) return;
            JsonNode item = entry.getValue();
            if (!"image".equals(text(item, "mediaFormat"))) return;
            String url = text(item, "thumbnailUrl");
            if (url != null && url.startsWith("https://")) urls.add(displayImageUrl(url));
        });
        menus.stream().map(NaverMenuItem::imageUrl).filter(url -> url != null && url.startsWith("https://"))
                .limit(Math.max(0, 30 - urls.size())).forEach(urls::add);
        return List.copyOf(urls);
    }

    private static String displayImageUrl(String url) {
        URI uri = Hosts.parseHttps(url);
        if (uri == null || !"blogfiles.pstatic.net".equalsIgnoreCase(uri.getHost())) return url;
        return "https://search.pstatic.net/common/?autoRotate=true&type=w560_sharpen&src="
                + URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    private static List<NaverMenuItem> menuItems(JsonNode state) {
        Set<String> itemIds = new LinkedHashSet<>();
        Map<String, LinkedHashSet<String>> itemCategories = new LinkedHashMap<>();
        state.properties().forEach(entry -> {
            if (!entry.getKey().startsWith("PlaceMenuCategory:")) return;
            String category = text(entry.getValue(), "name");
            JsonNode ids = entry.getValue().path("itemIds");
            if (ids.isArray()) ids.forEach(id -> {
                if (!id.isTextual()) return;
                itemIds.add(id.asText());
                if (category != null) {
                    itemCategories.computeIfAbsent(id.asText(), ignored -> new LinkedHashSet<>()).add(category);
                }
            });
        });
        // 카테고리에 연결되지 않은 메뉴도 누락하지 않는다.
        state.propertyNames().forEach(key -> {
            if (key.startsWith("PlaceMenuItem:")) itemIds.add(key.substring("PlaceMenuItem:".length()));
        });

        List<NaverMenuItem> menus = new ArrayList<>();
        for (String itemId : itemIds) {
            JsonNode item = state.path("PlaceMenuItem:" + itemId);
            String menuName = text(item, "name");
            if (menuName == null) continue;
            String imageUrl = text(item, "thumbnailUrl");
            if (imageUrl != null && !imageUrl.startsWith("https://")) imageUrl = null;
            List<String> categories = List.copyOf(itemCategories.getOrDefault(itemId, new LinkedHashSet<>()));
            menus.add(new NaverMenuItem(menuName, text(item.path("price"), "displayText"),
                    text(item, "description"), imageUrl,
                    categories.isEmpty() ? null : categories.get(0), categories));
        }
        return List.copyOf(menus);
    }

    private static List<String> menuImageUrls(JsonNode state) {
        JsonNode images = state.findValue("menuImages");
        if (images == null || !images.isArray()) return List.of();
        Set<String> urls = new LinkedHashSet<>();
        images.forEach(image -> {
            String url = text(image, "imageUrl");
            if (url != null && url.startsWith("https://")) urls.add(url);
        });
        return List.copyOf(urls);
    }

    private List<NaverMenuItem> fetchFullMenus(String placeId, List<NaverMenuItem> fallback) {
        try {
            MapHttpFetcher.MapHttpResponse response = fetcher.get(
                    URI.create("https://m.place.naver.com/place/" + placeId + "/menu/list"),
                    PLACE_HEADERS, this::isNaver);
            if (!response.ok()) return fallback;
            List<NaverMenuItem> menus = menuItems(readApolloState(response.body()));
            return menus.size() > fallback.size() ? menus : fallback;
        } catch (MapProviderConnectionException exception) {
            return fallback;
        }
    }

    private static JsonNode firstBusinessHoursGroup(JsonNode state) {
        JsonNode groups = state.findValue("newBusinessHours");
        return groups != null && groups.isArray() && !groups.isEmpty()
                ? groups.path(0) : state.path("__missing_business_hours__");
    }

    private static JsonNode businessStatus(JsonNode state) {
        return firstBusinessHoursGroup(state).path("businessStatusDescription");
    }

    private static List<NaverBusinessHour> businessHours(JsonNode state) {
        JsonNode items = firstBusinessHoursGroup(state).path("businessHours");
        if (!items.isArray()) return List.of();
        List<NaverBusinessHour> result = new ArrayList<>();
        items.forEach(item -> {
            String day = text(item, "day");
            if (day == null) return;
            JsonNode time = item.path("businessHours");
            String start = text(time, "start");
            String end = text(time, "end");
            String hours = start != null && end != null ? start + " - " + end : null;
            result.add(new NaverBusinessHour(day, hours, text(item, "description")));
        });
        return List.copyOf(result);
    }
}
