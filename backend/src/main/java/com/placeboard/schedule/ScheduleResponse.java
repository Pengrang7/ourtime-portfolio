package com.placeboard.schedule;

import java.time.LocalDateTime;

public record ScheduleResponse(
        long scheduleId,
        long boardId,
        Long boardPlaceId,
        String title,
        String placeName,
        String placeAddress,
        Double latitude,
        Double longitude,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        String memo,
        String color,
        String tagIds,
        String tagNames,
        String tagIcons,
        long createdBy,
        String createdByNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
