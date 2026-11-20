package com.placeboard.place;

import java.util.List;

public record NaverMenuItem(
        String name,
        String price,
        String description,
        String imageUrl,
        String category,
        List<String> categories
) {
}
