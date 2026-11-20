package com.placeboard.map;

import tools.jackson.databind.ObjectMapper;
import com.placeboard.place.PlacePreviewResponse;
import com.placeboard.place.UnsupportedMapUrlException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoMapProviderTest {
    private static final String PANEL_URL = "https://place-api.map.kakao.com/places/panel3/27138541";
    private static final String PANEL_JSON = """
            {"summary":{"confirm_id":"27138541","name":"월배요양병원",
             "category":{"name":"노인,요양병원","name3":"노인,요양병원"},
             "point":{"lon":128.52729531070747,"lat":35.815163538609454},
             "address":{"road":"대구 달서구 월배로 119 KS빌딩 3,5~8층","jibun":"진천동 92-9"},
             "phone_numbers":[{"tel":"053-634-0011","type":"FAX"},{"tel":"053-632-0808"}]}}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesShortUrlAndFetchesPlaceDetail() {
        FakeMapHttpFetcher fetcher = new FakeMapHttpFetcher()
                .redirect("https://kko.kakao.com/abc", "https://place.map.kakao.com/27138541")
                .body(PANEL_URL, PANEL_JSON);
        KakaoMapProvider provider = new KakaoMapProvider(fetcher, objectMapper);

        PlacePreviewResponse response = provider.parse("https://kko.kakao.com/abc");

        assertThat(response.provider()).isEqualTo(MapProviderType.KAKAO);
        assertThat(response.providerPlaceId()).isEqualTo("27138541");
        assertThat(response.name()).isEqualTo("월배요양병원");
        assertThat(response.address()).isEqualTo("대구 달서구 월배로 119 KS빌딩 3,5~8층");
        assertThat(response.latitude()).isEqualByComparingTo("35.815163538609454");
        assertThat(response.sido()).isEqualTo("대구광역시");
        assertThat(response.sigungu()).isEqualTo("달서구");
        assertThat(response.dong()).isEqualTo("진천동");
        assertThat(response.phone()).isEqualTo("053-632-0808");
    }

    @Test
    void extractsPlaceIdFromKnownUrlShapes() {
        assertThat(KakaoMapProvider.extractPlaceId(URI.create("https://place.map.kakao.com/27138541"))).isEqualTo("27138541");
        assertThat(KakaoMapProvider.extractPlaceId(URI.create("https://place.map.kakao.com/m/27138541"))).isEqualTo("27138541");
        assertThat(KakaoMapProvider.extractPlaceId(URI.create("https://map.kakao.com/?itemId=27138541&q=x"))).isEqualTo("27138541");
        assertThat(KakaoMapProvider.extractPlaceId(URI.create("https://map.kakao.com/link/map/27138541"))).isEqualTo("27138541");
        assertThatThrownBy(() -> KakaoMapProvider.extractPlaceId(URI.create("https://map.kakao.com/?q=cafe")))
                .isInstanceOf(UnsupportedMapUrlException.class);
    }

    @Test
    void supportsOnlyKakaoHosts() {
        KakaoMapProvider provider = new KakaoMapProvider(new FakeMapHttpFetcher(), objectMapper);

        assertThat(provider.supports("https://kko.to/abc")).isTrue();
        assertThat(provider.supports("https://place.map.kakao.com/123")).isTrue();
        assertThat(provider.supports("https://place.map.kakao.com.evil.example/123")).isFalse();
        assertThat(provider.supports("http://place.map.kakao.com/123")).isFalse();
    }
}
