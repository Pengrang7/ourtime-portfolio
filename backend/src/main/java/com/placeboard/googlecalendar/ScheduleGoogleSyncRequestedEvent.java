package com.placeboard.googlecalendar;

public record ScheduleGoogleSyncRequestedEvent(long scheduleId, Action action) {
    public enum Action { UPSERT, DELETE }
}
