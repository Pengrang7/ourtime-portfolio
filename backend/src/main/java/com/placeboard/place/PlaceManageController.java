package com.placeboard.place;

import com.placeboard.auth.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceManageController {
    private final BoardPlaceService boardPlaceService;

    public PlaceManageController(BoardPlaceService boardPlaceService) {
        this.boardPlaceService = boardPlaceService;
    }

    @PatchMapping("/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable long placeId,
            @RequestParam long boardId,
            @Valid @RequestBody PlaceUpdateRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        boardPlaceService.updatePlace(boardId, placeId, request, email, nickname);
    }
}
