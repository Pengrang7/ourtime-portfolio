package com.placeboard.map;

import tools.jackson.databind.ObjectMapper;
import com.placeboard.common.MapProviderConnectionException;
import com.placeboard.place.PlacePreviewResponse;
import com.placeboard.place.PlaceUnavailableException;
import com.placeboard.place.UnsupportedMapUrlException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaverMapProviderTest {
    private static final String DETAIL_URL = "https://m.place.naver.com/place/1610498965/home";
    private static final String MENU_URL = "https://m.place.naver.com/place/1610498965/menu/list";
    private static final String DETAIL_HTML = """
            <html><head><meta property="og:title" content="티원카페&amp;아레나 : 네이버"/></head>
            <script>window.__APOLLO_STATE__ = {"PlaceDetailBase:1610498965":{"id":"1610498965",
            "name":"티원카페&아레나","category":"카페,디저트","roadAddress":"서울 강남구 선릉로115길 4 1층",
            "address":"서울 강남구 역삼동 267-22","phone":null,"virtualPhone":"0507-1234-5678",
            "visitorReviewsTotal":2107,"cafeBlogReviewsTotal":270,"visitorReviewsScore":4.93,
            "microReviews":["친절한 서비스와 맛있는 음료의 만남"],"conveniences":["포장","무선 인터넷"],
            "coordinate":{"x":"127.0426383","y":"37.5124774"}},
            "PlaceDetailTopPhotoItem:1610498965_business_1":{"mediaFormat":"image","thumbnailUrl":"https://example.com/one.jpg"},
            "PlaceDetailTopPhotoItem:1610498965_clip_1":{"mediaFormat":"video","thumbnailUrl":"https://example.com/video.jpg"},
            "PlaceDetailTopPhotoItem:1610498965_visitor_1":{"mediaFormat":"image","thumbnailUrl":"https://blogfiles.pstatic.net/two.jpg"},
            "PlaceMenuCategory:u.p.1610498965":{"itemIds":["menu-1"]},
            "PlaceMenuItem:menu-1":{"name":"아메리카노","description":"고소한 원두","thumbnailUrl":"https://example.com/menu.jpg","price":{"displayText":"5,000원"}},
            "ROOT_QUERY":{"placeDetail":{"placeMenus":{"menuCount":3},"menuImages":[{"imageUrl":"https://example.com/menu-board.jpg"}],
            "newBusinessHours":[{"businessStatusDescription":{"status":"영업 중","description":"22:00에 영업 종료"},
            "businessHours":[{"day":"월","businessHours":{"start":"10:00","end":"22:00"},"description":null},
            {"day":"화","businessHours":null,"description":"정기휴무"}]}]}}};
            window.__OTHER__ = {};</script></html>
            """;
    private static final String MENU_HTML = """
            <html><script>window.__APOLLO_STATE__ = {
            "PlaceMenuCategory:recommend":{"name":"추천 메뉴","itemIds":["menu-1"]},
            "PlaceMenuCategory:coffee":{"name":"coffee","itemIds":["menu-1","menu-2"]},
            "PlaceMenuCategory:dessert":{"name":"dessert","itemIds":["menu-3"]},
            "PlaceMenuItem:menu-1":{"name":"아메리카노","description":"고소한 원두","thumbnailUrl":"https://example.com/menu.jpg","price":{"displayText":"5,000원"}},
            "PlaceMenuItem:menu-2":{"name":"라떼","thumbnailUrl":"https://example.com/latte.jpg","price":{"displayText":"5,500원"}},
            "PlaceMenuItem:menu-3":{"name":"바닐라 라떼","thumbnailUrl":"https://example.com/vanilla.jpg","price":{"displayText":"6,500원"}}};
            window.__OTHER__ = {};</script></html>
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesShortUrlAndFetchesPlaceDetail() {
        FakeMapHttpFetcher fetcher = new FakeMapHttpFetcher()
                .redirect("https://naver.me/GXApFSiE", "https://map.naver.com/p/entry/place/1610498965?placePath=%2Fhome")
                .body(DETAIL_URL, DETAIL_HTML)
                .body(MENU_URL, MENU_HTML);
        NaverMapProvider provider = new NaverMapProvider(fetcher, objectMapper);

        PlacePreviewResponse response = provider.parse("https://naver.me/GXApFSiE");

        assertThat(response.provider()).isEqualTo(MapProviderType.NAVER);
        assertThat(response.providerPlaceId()).isEqualTo("1610498965");
        assertThat(response.originalUrl()).isEqualTo("https://naver.me/GXApFSiE");
        assertThat(response.name()).isEqualTo("티원카페&아레나");
        assertThat(response.address()).isEqualTo("서울 강남구 선릉로115길 4 1층");
        assertThat(response.latitude()).isEqualByComparingTo("37.5124774");
        assertThat(response.longitude()).isEqualByComparingTo("127.0426383");
        assertThat(response.sido()).isEqualTo("서울특별시");
        assertThat(response.sigungu()).isEqualTo("강남구");
        assertThat(response.dong()).isEqualTo("역삼동");
        assertThat(response.category()).isEqualTo("카페,디저트");
        assertThat(response.phone()).isEqualTo("0507-1234-5678");
        assertThat(response.naverDetails().imageUrls()).containsExactly(
                "https://example.com/one.jpg",
                "https://search.pstatic.net/common/?autoRotate=true&type=w560_sharpen&src=https%3A%2F%2Fblogfiles.pstatic.net%2Ftwo.jpg",
                "https://example.com/menu.jpg",
                "https://example.com/latte.jpg", "https://example.com/vanilla.jpg");
        assertThat(response.naverDetails().visitorReviewsTotal()).isEqualTo(2107);
        assertThat(response.naverDetails().blogReviewsTotal()).isEqualTo(270);
        assertThat(response.naverDetails().visitorReviewScore()).isEqualByComparingTo("4.93");
        assertThat(response.naverDetails().microReviews()).containsExactly("친절한 서비스와 맛있는 음료의 만남");
        assertThat(response.naverDetails().conveniences()).containsExactly("포장", "무선 인터넷");
        assertThat(response.naverDetails().businessStatus()).isEqualTo("영업 중");
        assertThat(response.naverDetails().businessStatusDescription()).isEqualTo("22:00에 영업 종료");
        assertThat(response.naverDetails().businessHours())
                .containsExactly(new com.placeboard.place.NaverBusinessHour("월", "10:00 - 22:00", null),
                        new com.placeboard.place.NaverBusinessHour("화", null, "정기휴무"));
        assertThat(response.naverDetails().menus())
                .containsExactly(
                        new com.placeboard.place.NaverMenuItem(
                                "아메리카노", "5,000원", "고소한 원두", "https://example.com/menu.jpg", "추천 메뉴",
                                java.util.List.of("추천 메뉴", "coffee")),
                        new com.placeboard.place.NaverMenuItem(
                                "라떼", "5,500원", null, "https://example.com/latte.jpg", "coffee",
                                java.util.List.of("coffee")),
                        new com.placeboard.place.NaverMenuItem(
                                "바닐라 라떼", "6,500원", null, "https://example.com/vanilla.jpg", "dessert",
                                java.util.List.of("dessert")));
        assertThat(response.naverDetails().menuImageUrls())
                .containsExactly("https://example.com/menu-board.jpg");
    }

    @Test
    void extractsPlaceIdFromDirectUrlWithoutRedirect() {
        FakeMapHttpFetcher fetcher = new FakeMapHttpFetcher()
                .body(DETAIL_URL, DETAIL_HTML)
                .body(MENU_URL, MENU_HTML);
        NaverMapProvider provider = new NaverMapProvider(fetcher, objectMapper);

        provider.parse("https://m.place.naver.com/restaurant/1610498965/home");

        assertThat(fetcher.requested).containsExactly(DETAIL_URL, MENU_URL);
    }

    @Test
    void extractsPlaceIdFromMobileAppLink() {
        assertThat(NaverMapProvider.extractPlaceId(java.net.URI.create(
                "https://m.map.naver.com/appLink.naver?pinId=1610498965&pinType=site"))).isEqualTo("1610498965");
    }

    @Test
    void rejectsUrlWithoutPlaceId() {
        NaverMapProvider provider = new NaverMapProvider(new FakeMapHttpFetcher(), objectMapper);

        assertThatThrownBy(() -> provider.parse("https://map.naver.com/p/search/cafe"))
                .isInstanceOf(UnsupportedMapUrlException.class)
                .hasMessage("네이버 장소 ID를 찾을 수 없습니다.");
    }

    @Test
    void reportsBlockedDetailRequest() {
        FakeMapHttpFetcher fetcher = new FakeMapHttpFetcher().status(DETAIL_URL, 429);
        NaverMapProvider provider = new NaverMapProvider(fetcher, objectMapper);

        assertThatThrownBy(() -> provider.parse("https://map.naver.com/p/entry/place/1610498965"))
                .isInstanceOf(MapProviderConnectionException.class);
    }

    @Test
    void reportsRemovedPlace() {
        FakeMapHttpFetcher fetcher = new FakeMapHttpFetcher().status(DETAIL_URL, 404);
        NaverMapProvider provider = new NaverMapProvider(fetcher, objectMapper);

        assertThatThrownBy(() -> provider.parse("https://map.naver.com/p/entry/place/1610498965"))
                .isInstanceOf(PlaceUnavailableException.class);
    }

    @Test
    void supportsOnlyHttpsNaverMapHosts() {
        NaverMapProvider provider = new NaverMapProvider(new FakeMapHttpFetcher(), objectMapper);

        assertThat(provider.supports("https://naver.me/example")).isTrue();
        assertThat(provider.supports("https://m.place.naver.com/place/123")).isTrue();
        assertThat(provider.supports("http://naver.me/example")).isFalse();
        assertThat(provider.supports("https://naver.me.evil.example/place/123")).isFalse();
    }
}
