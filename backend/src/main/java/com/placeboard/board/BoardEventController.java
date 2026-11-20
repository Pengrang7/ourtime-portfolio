package com.placeboard.board;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/boards/{boardId}/events")
public class BoardEventController {
    private final BoardService boardService;
    private final CurrentUserService currentUserService;
    private final BoardEventStream eventStream;

    public BoardEventController(BoardService boardService, CurrentUserService currentUserService,
                                BoardEventStream eventStream) {
        this.boardService = boardService;
        this.currentUserService = currentUserService;
        this.eventStream = eventStream;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable long boardId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname,
            HttpServletResponse response
    ) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return eventStream.subscribe(boardId, user.userId());
    }
}
