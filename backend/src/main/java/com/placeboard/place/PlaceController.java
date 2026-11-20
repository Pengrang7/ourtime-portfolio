package com.placeboard.place;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    private final PlacePreviewService placePreviewService;

    public PlaceController(PlacePreviewService placePreviewService) {
        this.placePreviewService = placePreviewService;
    }

    @PostMapping("/preview")
    public ResponseEntity<PlacePreviewResponse> preview(@Valid @RequestBody PlacePreviewRequest request) {
        return ResponseEntity.ok(placePreviewService.preview(request.url()));
    }
}
