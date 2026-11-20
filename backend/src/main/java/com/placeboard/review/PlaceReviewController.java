package com.placeboard.review;

import com.placeboard.auth.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/places/{boardPlaceId}/reviews")
public class PlaceReviewController {
    private final PlaceReviewService reviewService;

    public PlaceReviewController(PlaceReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<PlaceReviewResponse> findAll(
            @PathVariable long boardId, @PathVariable long boardPlaceId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return reviewService.findAll(boardId, boardPlaceId, email, nickname);
    }

    @PutMapping("/me")
    public PlaceReviewResponse saveMine(
            @PathVariable long boardId, @PathVariable long boardPlaceId,
            @Valid @RequestBody PlaceReviewRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return reviewService.saveMine(boardId, boardPlaceId, request, email, nickname);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMine(
            @PathVariable long boardId, @PathVariable long boardPlaceId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        reviewService.deleteMine(boardId, boardPlaceId, email, nickname);
    }
}
