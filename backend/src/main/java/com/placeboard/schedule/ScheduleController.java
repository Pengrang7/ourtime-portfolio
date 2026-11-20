package com.placeboard.schedule;

import com.placeboard.auth.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleResponse> findAll(
            @PathVariable long boardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return scheduleService.findAll(boardId, from, to, email, nickname);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(
            @PathVariable long boardId, @Valid @RequestBody ScheduleRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return scheduleService.create(boardId, request, email, nickname);
    }

    @PatchMapping("/{scheduleId}")
    public ScheduleResponse update(
            @PathVariable long boardId, @PathVariable long scheduleId,
            @Valid @RequestBody ScheduleRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return scheduleService.update(boardId, scheduleId, request, email, nickname);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable long boardId, @PathVariable long scheduleId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        scheduleService.delete(boardId, scheduleId, email, nickname);
    }
}
