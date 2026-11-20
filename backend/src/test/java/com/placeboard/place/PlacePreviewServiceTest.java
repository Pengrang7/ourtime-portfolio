package com.placeboard.place;

import com.placeboard.map.MapProvider;
import com.placeboard.map.MapProviderType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlacePreviewServiceTest {
    @Test
    void selectsSupportingProvider() {
        MapProvider provider = new MapProvider() {
            public boolean supports(String url) {
                return url.startsWith("https://naver.me/");
            }

            public PlacePreviewResponse parse(String url) {
                return PlacePreviewResponse.of(MapProviderType.NAVER, "123", url, "장소", "서울 중구 세종대로 110", null,
                        null, null, null, null);
            }
        };
        PlacePreviewService service = new PlacePreviewService(List.of(provider));

        PlacePreviewResponse response = service.preview("  https://naver.me/test  ");

        assertThat(response.providerPlaceId()).isEqualTo("123");
        assertThat(response.originalUrl()).isEqualTo("https://naver.me/test");
    }

    @Test
    void rejectsUnsupportedProvider() {
        PlacePreviewService service = new PlacePreviewService(List.of());

        assertThatThrownBy(() -> service.preview("https://example.com/place/123"))
                .isInstanceOf(UnsupportedMapUrlException.class)
                .hasMessage("지원하지 않는 지도 URL입니다.");
    }
}
