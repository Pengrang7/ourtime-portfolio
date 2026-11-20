package com.placeboard.cycle;

import com.placeboard.auth.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/cycle-periods")
public class BoardCyclePeriodController {
    private final CyclePeriodService cyclePeriodService;

    public BoardCyclePeriodController(CyclePeriodService cyclePeriodService) {
        this.cyclePeriodService = cyclePeriodService;
    }

    @GetMapping
    public List<BoardCyclePeriodResponse> findForBoard(
            @PathVariable long boardId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return cyclePeriodService.findForBoard(boardId, email, nickname);
    }
}
