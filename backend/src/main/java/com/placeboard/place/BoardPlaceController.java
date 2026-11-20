package com.placeboard.place;

import com.placeboard.auth.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/places")
public class BoardPlaceController {
    private final BoardPlaceService boardPlaceService;

    public BoardPlaceController(BoardPlaceService boardPlaceService) {
        this.boardPlaceService = boardPlaceService;
    }

    @GetMapping
    public List<PlaceListItem> findAll(
            @PathVariable long boardId,
            @RequestParam(required = false) com.placeboard.review.RevisitFilter revisit,
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardPlaceService.findAll(boardId, revisit, sido, sigungu, tagId, keyword, email, nickname);
    }

    @GetMapping("/regions")
    public List<PlaceRegionOption> findRegionOptions(
            @PathVariable long boardId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardPlaceService.findRegionOptions(boardId, email, nickname);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceListItem save(
            @PathVariable long boardId,
            @Valid @RequestBody PlaceSaveRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardPlaceService.save(boardId, request, email, nickname);
    }

    @PatchMapping("/{boardPlaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable long boardId,
            @PathVariable long boardPlaceId,
            @Valid @RequestBody BoardPlaceUpdateRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        boardPlaceService.update(boardId, boardPlaceId, request, email, nickname);
    }

    @PostMapping("/{boardPlaceId}/refresh")
    public PlaceListItem refresh(
            @PathVariable long boardId,
            @PathVariable long boardPlaceId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardPlaceService.refresh(boardId, boardPlaceId, email, nickname);
    }

    @DeleteMapping("/{boardPlaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable long boardId,
            @PathVariable long boardPlaceId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        boardPlaceService.delete(boardId, boardPlaceId, email, nickname);
    }
}
