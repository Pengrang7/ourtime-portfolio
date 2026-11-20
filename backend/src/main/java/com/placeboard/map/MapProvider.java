package com.placeboard.map;

import com.placeboard.place.PlacePreviewResponse;

public interface MapProvider {
    boolean supports(String url);

    PlacePreviewResponse parse(String url);
}
