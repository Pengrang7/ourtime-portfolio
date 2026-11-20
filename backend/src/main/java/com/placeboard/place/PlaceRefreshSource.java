package com.placeboard.place;

public record PlaceRefreshSource(
        long placeId,
        String provider,
        String providerPlaceId,
        String originalUrl
) {
}

