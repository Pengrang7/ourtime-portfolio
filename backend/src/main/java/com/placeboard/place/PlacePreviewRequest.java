package com.placeboard.place;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlacePreviewRequest(
        @NotBlank(message = "지도 URL을 입력해 주세요.")
        @Size(max = 2048, message = "URL은 2,048자 이하여야 합니다.") String url
) {
}
