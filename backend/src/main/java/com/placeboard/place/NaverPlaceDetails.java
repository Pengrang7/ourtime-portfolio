package com.placeboard.place;

import java.math.BigDecimal;
import java.util.List;

public record NaverPlaceDetails(
        List<String> imageUrls,
        Integer visitorReviewsTotal,
        Integer blogReviewsTotal,
        BigDecimal visitorReviewScore,
        List<String> microReviews,
        List<String> conveniences,
        String businessStatus,
        String businessStatusDescription,
        List<NaverBusinessHour> businessHours,
        List<NaverMenuItem> menus,
        List<String> menuImageUrls
) {
}
