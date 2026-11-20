package com.placeboard.map;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import com.placeboard.place.PlacePreviewResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleMapProviderTest {
    private static final String PLACE_URL = "https://www.google.com/maps/place/%EA%B2%BD%EB%B3%B5%EA%B6%81"
            + "/@37.579617,126.977041,17z/data=!3m1!4b1!4m6!3m5!1s0x357ca2c74aeddea1:0x8b3046532cc715f6"
            + "!8m2!3d37.579617!4d126.977041";
    private static final String PREVIEW_PATH = "/maps/preview/place?hl=ko&pb=!1m1";
    private static final String PREVIEW_BODY = ")]}'\n[null,null,null,null,null,null,[\"a\",\"b\",[\"서울특별시 종로구 사직로 161\"],"
            + "null,null,null,null,null,null,[null,null,37.579617,126.977041],\"0x357ca2c74aeddea1:0x8b3046532cc715f6\","
            + "\"경복궁\",null,[\"문화적 랜드마크\"]]]";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsPlaceByFeatureIdFromResolvedUrl() {
        String expectedPreview = "https://www.google.com/maps/preview/place?authuser=0&hl=ko&gl=kr"
                + "&pb=!1m1!1s0x357ca2c74aeddea1:0x8b3046532cc715f6";
        PlacePreviewResponse response = parseWith(PLACE_URL, "", expectedPreview);

        assertThat(response.provider()).isEqualTo(MapProviderType.GOOGLE);
        assertThat(response.providerPlaceId()).isEqualTo("0x357ca2c74aeddea1:0x8b3046532cc715f6");
        assertThat(response.name()).isEqualTo("경복궁");
        assertThat(response.address()).isEqualTo("서울특별시 종로구 사직로 161");
        assertThat(response.latitude()).isEqualByComparingTo("37.579617");
        assertThat(response.sido()).isEqualTo("서울특별시");
        assertThat(response.sigungu()).isEqualTo("종로구");
        assertThat(response.category()).isEqualTo("문화적 랜드마크");
    }

    @Test
    void fallsBackToEmbeddedPreviewLinkWithoutFeatureId() {
        String searchUrl = "https://www.google.com/maps/search/%EA%B2%BD%EB%B3%B5%EA%B6%81";
        String html = "<a href=\"" + PREVIEW_PATH.replace("&", "&amp;") + "\">";
        PlacePreviewResponse response = parseWith(searchUrl, html, "https://www.google.com" + PREVIEW_PATH);

        assertThat(response.name()).isEqualTo("경복궁");
        assertThat(response.providerPlaceId()).isEqualTo("0x357ca2c74aeddea1:0x8b3046532cc715f6");
    }

    @Test
    void readsGooglePlaceDetails() {
        String expectedPreview = "https://www.google.com/maps/preview/place?authuser=0&hl=ko&gl=kr"
                + "&pb=!1m1!1s0x357ca2c74aeddea1:0x8b3046532cc715f6";
        PlacePreviewResponse response = parseWith(PLACE_URL, "", expectedPreview, detailedPreviewBody());

        assertThat(response.phone()).isEqualTo("02-3700-3900");
        assertThat(response.googleDetails().googlePlaceId()).isEqualTo("ChIJod7tSseifDUR9hXHLFNGMIs");
        assertThat(response.googleDetails().rating()).isEqualByComparingTo("4.6");
        assertThat(response.googleDetails().reviewsTotal()).isEqualTo(46921);
        assertThat(response.googleDetails().description()).isEqualTo("역사적인 궁전");
        assertThat(response.googleDetails().businessStatus()).isEqualTo("영업 중");
        assertThat(response.googleDetails().businessHours()).singleElement()
                .satisfies(hour -> {
                    assertThat(hour.day()).isEqualTo("목요일");
                    assertThat(hour.hours()).isEqualTo("오전 9:00~오후 6:00");
                });
        assertThat(response.googleDetails().websiteUrl()).isEqualTo("https://example.com/menu?id=1");
    }

    private PlacePreviewResponse parseWith(String resolvedUrl, String pageHtml, String expectedPreviewUrl) {
        return parseWith(resolvedUrl, pageHtml, expectedPreviewUrl, PREVIEW_BODY);
    }

    private PlacePreviewResponse parseWith(String resolvedUrl, String pageHtml, String expectedPreviewUrl,
                                           String previewBody) {
        MapHttpFetcher fetcher = new MapHttpFetcher() {
            public MapHttpResponse get(URI uri, Map<String, String> headers, Predicate<URI> allowedUri) {
                if (uri.getHost().equals("maps.app.goo.gl")) {
                    return new MapHttpResponse(200, URI.create(resolvedUrl), pageHtml);
                }
                assertThat(uri.toString()).isEqualTo(expectedPreviewUrl);
                return new MapHttpResponse(200, uri, previewBody);
            }
        };
        return new GoogleMapProvider(fetcher, objectMapper).parse("https://maps.app.goo.gl/abc");
    }

    private String detailedPreviewBody() {
        ArrayNode root = objectMapper.createArrayNode();
        ArrayNode place = objectMapper.createArrayNode();
        ensureSize(root, 6);
        ensureSize(place, 203);
        root.set(6, place);

        place.set(2, objectMapper.createArrayNode().add("서울특별시 종로구 사직로 161"));
        ArrayNode reviews = objectMapper.createArrayNode();
        ensureSize(reviews, 8);
        reviews.set(7, objectMapper.getNodeFactory().numberNode(4.6));
        reviews.set(8, objectMapper.getNodeFactory().numberNode(46921));
        place.set(4, reviews);
        place.set(7, objectMapper.createArrayNode().add("/url?q=https%3A%2F%2Fexample.com%2Fmenu%3Fid%3D1&sa=U"));
        place.set(9, objectMapper.createArrayNode().addNull().addNull().add(37.579617).add(126.977041));
        place.set(10, objectMapper.getNodeFactory().textNode("0x357ca2c74aeddea1:0x8b3046532cc715f6"));
        place.set(11, objectMapper.getNodeFactory().textNode("경복궁"));
        place.set(13, objectMapper.createArrayNode().add("문화적 랜드마크"));
        ArrayNode descriptions = objectMapper.createArrayNode();
        descriptions.addNull();
        descriptions.add(objectMapper.createArrayNode().addNull().add("역사적인 궁전"));
        place.set(32, descriptions);
        place.set(39, objectMapper.getNodeFactory().textNode("서울특별시 종로구 사직로 161"));
        place.set(78, objectMapper.getNodeFactory().textNode("ChIJod7tSseifDUR9hXHLFNGMIs"));
        place.set(178, objectMapper.createArrayNode().add(objectMapper.createArrayNode().add("02-3700-3900")));

        ArrayNode day = objectMapper.createArrayNode().add("목요일").add(4).addNull();
        day.add(objectMapper.createArrayNode().add(objectMapper.createArrayNode().add("오전 9:00~오후 6:00")));
        ArrayNode current = objectMapper.createArrayNode();
        ensureSize(current, 8);
        current.set(4, objectMapper.createArrayNode().add("영업 중 · 오후 6:00에 영업 종료"));
        current.set(8, objectMapper.createArrayNode().add("영업 중"));
        place.set(203, objectMapper.createArrayNode().add(objectMapper.createArrayNode().add(day)).add(current));

        return ")]}'\n" + root;
    }

    private static void ensureSize(ArrayNode node, int lastIndex) {
        while (node.size() <= lastIndex) node.addNull();
    }

    @Test
    void supportsGoogleMapsUrlsOnly() {
        GoogleMapProvider provider = new GoogleMapProvider(new FakeMapHttpFetcher(), objectMapper);

        assertThat(provider.supports("https://maps.app.goo.gl/abc")).isTrue();
        assertThat(provider.supports("https://www.google.com/maps/place/abc")).isTrue();
        assertThat(provider.supports("https://goo.gl/maps/abc")).isTrue();
        assertThat(provider.supports("https://www.google.com/search?q=abc")).isFalse();
        assertThat(provider.supports("https://goo.gl/abc")).isFalse();
    }
}
