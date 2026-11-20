package com.placeboard.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BoardCreateRequest(
        @NotBlank(message = "보드 이름을 입력해 주세요.")
        @Size(max = 150, message = "보드 이름은 150자 이하여야 합니다.")
        String name,
        @NotNull(message = "국내 또는 해외 보드를 선택해 주세요.")
        BoardMapType mapType
) {
}
