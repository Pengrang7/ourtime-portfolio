package com.placeboard.place;

import java.math.BigDecimal;
import java.util.List;

public record GooglePlaceDetails(
        String googlePlaceId,
        BigDecimal rating,
        Integer reviewsTotal,
        String description,
        String businessStatus,
        String businessStatusDescription,
        List<NaverBusinessHour> businessHours,
        String websiteUrl
) {
}
