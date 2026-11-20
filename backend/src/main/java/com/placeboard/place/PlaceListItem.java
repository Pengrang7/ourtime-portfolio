package com.placeboard.place;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlaceListItem(
        long boardPlaceId,
        long placeId,
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
        String provider,
        String providerPlaceId,
        String originalUrl,
        PlaceSourceStatus sourceStatus,
        LocalDateTime lastVerifiedAt,
        int revisitYesCount,
        int revisitMaybeCount,
        int revisitNoCount,
        long createdBy,
        String createdByNickname,
        LocalDateTime createdAt,
        String tagNames
) {
}
