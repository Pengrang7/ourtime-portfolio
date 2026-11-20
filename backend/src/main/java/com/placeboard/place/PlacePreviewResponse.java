package com.placeboard.place;

import com.placeboard.map.KoreanAddressParser;
import com.placeboard.map.MapProviderType;

import java.math.BigDecimal;

public record PlacePreviewResponse(
        MapProviderType provider,
        String providerPlaceId,
        String originalUrl,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String country,
        String sido,
        String sigungu,
        String dong,
        String category,
        String phone,
        NaverPlaceDetails naverDetails,
        GooglePlaceDetails googleDetails
) {
    public static PlacePreviewResponse of(MapProviderType provider, String providerPlaceId, String originalUrl,
                                          String name, String roadAddress, String jibunAddress,
                                          BigDecimal latitude, BigDecimal longitude,
                                          String category, String phone) {
        String address = blankToNull(roadAddress) != null ? roadAddress.trim() : blankToNull(jibunAddress);
        if (blankToNull(name) == null || address == null) {
            throw new PlaceUnavailableException("더 이상 장소 정보를 확인할 수 없습니다.");
        }
        KoreanAddressParser.Region region = KoreanAddressParser.parse(roadAddress, jibunAddress);
        return new PlacePreviewResponse(provider, providerPlaceId, originalUrl, name.trim(), address,
                latitude, longitude, region.country(), region.sido(), region.sigungu(), region.dong(),
                blankToNull(category), blankToNull(phone), null, null);
    }

    public PlacePreviewResponse withNaverDetails(NaverPlaceDetails details) {
        return new PlacePreviewResponse(provider, providerPlaceId, originalUrl, name, address,
                latitude, longitude, country, sido, sigungu, dong, category, phone, details, googleDetails);
    }

    public PlacePreviewResponse withGoogleDetails(GooglePlaceDetails details) {
        return new PlacePreviewResponse(provider, providerPlaceId, originalUrl, name, address,
                latitude, longitude, country, sido, sigungu, dong, category, phone, naverDetails, details);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
