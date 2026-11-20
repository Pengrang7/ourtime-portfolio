package com.placeboard.googlecalendar;

public record GoogleCalendarConnection(
        long userId,
        String encryptedRefreshToken,
        String calendarId
) {
}
