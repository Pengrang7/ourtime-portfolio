package com.placeboard.place;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BoardPlaceUpdateRequest(
        @Size(max = 20, message = "태그는 최대 20개까지 선택할 수 있습니다.")
        List<@NotNull(message = "올바른 태그를 선택해 주세요.")
             @Positive(message = "올바른 태그를 선택해 주세요.") Long> tagIds
) {
}
