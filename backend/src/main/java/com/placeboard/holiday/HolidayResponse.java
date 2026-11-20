package com.placeboard.holiday;

import java.time.LocalDate;

public record HolidayResponse(LocalDate date, String name) {
}
