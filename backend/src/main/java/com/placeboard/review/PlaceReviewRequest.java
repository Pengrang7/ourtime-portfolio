package com.placeboard.review;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlaceReviewRequest(
        @NotNull(message = "재방문 의사를 선택해 주세요.") RevisitIntention revisitIntention,
        @Size(max = 1000, message = "후기는 1,000자 이하로 입력해 주세요.") String content,
        @PastOrPresent(message = "방문일은 오늘 이후일 수 없습니다.") LocalDate visitedOn
) {
}
