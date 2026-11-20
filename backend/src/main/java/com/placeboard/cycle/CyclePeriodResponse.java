package com.placeboard.cycle;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CyclePeriodResponse(
        long periodId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
