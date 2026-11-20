package com.placeboard.cycle;

import com.placeboard.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CyclePeriodControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CyclePeriodService service = mock(CyclePeriodService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CyclePeriodController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsEndDateBeforeStartDate() throws Exception {
        mockMvc.perform(post("/api/users/me/cycle-periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-09-10","endDate":"2026-09-09"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("종료일은 시작일 이후 14일 이내여야 합니다."));
    }

    @Test
    void rejectsPeriodLongerThanFourteenDays() throws Exception {
        mockMvc.perform(post("/api/users/me/cycle-periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-09-01","endDate":"2026-09-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void acceptsFutureEndDateWithinFourteenDays() throws Exception {
        LocalDate startDate = LocalDate.now();

        mockMvc.perform(post("/api/users/me/cycle-periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"%s","endDate":"%s"}
                                """.formatted(startDate, startDate.plusDays(13))))
                .andExpect(status().isCreated());
    }
}
