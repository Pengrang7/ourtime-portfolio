package com.placeboard.map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanAddressParserTest {
    @Test
    void normalizesShortSidoAndFindsDong() {
        KoreanAddressParser.Region region = KoreanAddressParser.parse("서울 강남구 선릉로115길 4", "서울 강남구 역삼동 267-22");

        assertThat(region).isEqualTo(new KoreanAddressParser.Region("KR", "서울특별시", "강남구", "역삼동"));
    }

    @Test
    void combinesCityAndDistrict() {
        KoreanAddressParser.Region region = KoreanAddressParser.parse("경기 수원시 영통구 광교중앙로 140 (하동)", null);

        assertThat(region).isEqualTo(new KoreanAddressParser.Region("KR", "경기도", "수원시 영통구", "하동"));
    }

    @Test
    void stripsCountryPrefixAndSkipsSigunguForSejong() {
        assertThat(KoreanAddressParser.parse("대한민국 세종특별자치시 한누리대로 2130", null))
                .isEqualTo(new KoreanAddressParser.Region("KR", "세종특별자치시", null, null));
    }

    @Test
    void leavesForeignAddressEmpty() {
        assertThat(KoreanAddressParser.parse("1600 Amphitheatre Pkwy, Mountain View, CA", null))
                .isEqualTo(new KoreanAddressParser.Region(null, null, null, null));
    }
}
