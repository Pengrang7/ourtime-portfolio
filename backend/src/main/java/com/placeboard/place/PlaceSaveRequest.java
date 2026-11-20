package com.placeboard.place;

import com.placeboard.map.MapProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PlaceSaveRequest(
        @NotNull MapProviderType provider,
        @Size(max = 255) String providerPlaceId,
        @NotBlank @Size(max = 2048) String originalUrl,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 500) String address,
        BigDecimal latitude,
        BigDecimal longitude,
        @Size(max = 10) String country,
        @Size(max = 100) String sido,
        @Size(max = 100) String sigungu,
        @Size(max = 100) String dong,
        @Size(max = 200) String category,
        @Size(max = 50) String phone,
        @Size(max = 20, message = "태그는 최대 20개까지 선택할 수 있습니다.")
        List<@NotNull(message = "올바른 태그를 선택해 주세요.")
             @Positive(message = "올바른 태그를 선택해 주세요.") Long> tagIds
) {
}
