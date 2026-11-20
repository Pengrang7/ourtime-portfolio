package com.placeboard.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardNameRequest(
        @NotBlank(message = "보드 이름을 입력해 주세요.")
        @Size(max = 150, message = "보드 이름은 150자 이하여야 합니다.")
        String name
) {
}
