package com.placeboard.cycle;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BoardCyclePeriodResponse(
        long periodId,
        long userId,
        String nickname,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
