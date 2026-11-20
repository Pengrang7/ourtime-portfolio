package com.placeboard.place;

import com.placeboard.map.MapProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlacePreviewService {
    private final List<MapProvider> providers;

    public PlacePreviewService(List<MapProvider> providers) {
        this.providers = providers;
    }

    public PlacePreviewResponse preview(String url) {
        String normalizedUrl = url.trim();
        return providers.stream()
                .filter(provider -> provider.supports(normalizedUrl))
                .findFirst()
                .map(provider -> provider.parse(normalizedUrl))
                .orElseThrow(() -> new UnsupportedMapUrlException("지원하지 않는 지도 URL입니다."));
    }
}
