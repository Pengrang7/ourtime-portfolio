package com.placeboard.system;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemControllerTest {
    @Test
    void returnsUpStatus() {
        HealthResponse response = new SystemController().health();

        assertThat(response.status()).isEqualTo("UP");
    }
}
