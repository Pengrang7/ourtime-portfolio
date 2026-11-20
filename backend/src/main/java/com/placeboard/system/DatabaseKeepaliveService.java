package com.placeboard.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DatabaseKeepaliveService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseKeepaliveService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseKeepaliveService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(
            initialDelayString = "${placeboard.keepalive.initial-delay-ms:60000}",
            fixedDelayString = "${placeboard.keepalive.interval-ms:3000000}"
    )
    public void ping() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result == null || result != 1) {
                throw new IllegalStateException("데이터베이스 keepalive 응답이 올바르지 않습니다.");
            }
            log.info("데이터베이스 keepalive 성공");
        } catch (RuntimeException exception) {
            log.error("데이터베이스 keepalive 실패", exception);
        }
    }
}
