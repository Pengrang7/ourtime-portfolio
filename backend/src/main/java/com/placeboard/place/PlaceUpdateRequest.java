package com.placeboard.place;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlaceUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 500) String address,
        BigDecimal latitude,
        BigDecimal longitude,
        @Size(max = 10) String country,
        @Size(max = 100) String sido,
        @Size(max = 100) String sigungu,
        @Size(max = 100) String dong
) {
}
