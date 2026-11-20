package com.placeboard.googlecalendar;

public record CycleGoogleEvent(long sourceUserId, String eventKey, String googleEventId, String contentHash) {
}
