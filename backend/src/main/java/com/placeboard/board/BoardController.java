package com.placeboard.board;

import com.placeboard.auth.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {
    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public List<BoardSummary> findBoards(
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardService.findBoards(email, nickname);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardSummary create(
            @Valid @RequestBody BoardCreateRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardService.create(request, email, nickname);
    }

    @GetMapping("/{boardId}/members")
    public List<BoardMemberResponse> findMembers(
            @PathVariable long boardId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardService.findMembers(boardId, email, nickname);
    }

    @PostMapping("/{boardId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardMemberResponse invite(
            @PathVariable long boardId,
            @Valid @RequestBody MemberInviteRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardService.invite(boardId, request, email, nickname);
    }

    @PatchMapping("/{boardId}/members/{memberUserId}")
    public BoardMemberResponse updateMemberPermissions(
            @PathVariable long boardId,
            @PathVariable long memberUserId,
            @RequestBody MemberPermissionRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardService.updateMemberPermissions(boardId, memberUserId, request, email, nickname);
    }

    @PatchMapping("/{boardId}")
    public BoardSummary rename(
            @PathVariable long boardId,
            @Valid @RequestBody BoardNameRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        return boardService.rename(boardId, request, email, nickname);
    }

    @DeleteMapping("/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable long boardId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname
    ) {
        boardService.delete(boardId, email, nickname);
    }
}
