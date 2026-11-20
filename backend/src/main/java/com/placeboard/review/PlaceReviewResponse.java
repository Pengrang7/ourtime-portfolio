package com.placeboard.review;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlaceReviewResponse(
        long reviewId,
        long boardPlaceId,
        long userId,
        String nickname,
        RevisitIntention revisitIntention,
        String content,
        LocalDate visitedOn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
