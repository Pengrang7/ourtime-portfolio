package com.placeboard.schedule;

public record BoardSchedulesChangedEvent(long boardId, long changedByUserId) {
}
