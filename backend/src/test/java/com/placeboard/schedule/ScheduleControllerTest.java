package com.placeboard.schedule;

import com.placeboard.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ScheduleService scheduleService = mock(ScheduleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ScheduleController(scheduleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsEndTimeBeforeStartTime() throws Exception {
        mockMvc.perform(post("/api/boards/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardPlaceId": 2,
                                  "startAt": "2026-09-29T12:00:00",
                                  "endAt": "2026-09-29T11:00:00",
                                  "memo": "점심",
                                  "tagIds": [],
                                  "color": "#0B8043"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("종료 시간은 시작 시간보다 늦어야 합니다."));
    }

    @Test
    void allowsScheduleWithoutPlaceAndContent() throws Exception {
        mockMvc.perform(post("/api/boards/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-09-29T11:00:00",
                                  "endAt": "2026-09-29T12:00:00"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void allowsAllDayScheduleSpanningDates() throws Exception {
        mockMvc.perform(post("/api/boards/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-10-30T00:00:00",
                                  "endAt": "2026-11-01T00:00:00",
                                  "allDay": true
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsAllDayScheduleWithTime() throws Exception {
        mockMvc.perform(post("/api/boards/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-10-30T09:00:00",
                                  "endAt": "2026-10-31T00:00:00",
                                  "allDay": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("종일 일정은 날짜 단위로 입력해 주세요."));
    }

    @Test
    void rejectsMissingStartTime() throws Exception {
        mockMvc.perform(post("/api/boards/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endAt": "2026-09-29T12:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("시작 시간을 입력해 주세요."));
    }

    @Test
    void rejectsMalformedQueryDate() throws Exception {
        mockMvc.perform(get("/api/boards/1/schedules")
                        .param("from", "not-a-date")
                        .param("to", "2026-09-30T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsMissingQueryDate() throws Exception {
        mockMvc.perform(get("/api/boards/1/schedules")
                        .param("from", "2026-09-29T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/boards/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsInvalidPlaceAndTagIds() throws Exception {
        mockMvc.perform(post("/api/boards/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardPlaceId": 0,
                                  "startAt": "2026-09-29T11:00:00",
                                  "endAt": "2026-09-29T12:00:00",
                                  "tagIds": [null]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
