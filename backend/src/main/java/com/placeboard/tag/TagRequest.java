package com.placeboard.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(@NotBlank @Size(max = 100) String name, @Size(max = 16) String icon) {
}
