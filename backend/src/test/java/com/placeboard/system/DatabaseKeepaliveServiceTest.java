package com.placeboard.system;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseKeepaliveServiceTest {
    @Test
    void pingsDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        new DatabaseKeepaliveService(jdbcTemplate).ping();

        verify(jdbcTemplate).queryForObject("SELECT 1", Integer.class);
    }
}
