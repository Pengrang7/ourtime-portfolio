package com.placeboard.cycle;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/cycle-periods")
public class CyclePeriodController {
    private final CyclePeriodService cyclePeriodService;

    public CyclePeriodController(CyclePeriodService cyclePeriodService) {
        this.cyclePeriodService = cyclePeriodService;
    }

    @GetMapping
    public List<CyclePeriodResponse> findMine(
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return cyclePeriodService.findMine(email, nickname);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CyclePeriodResponse create(
            @Valid @RequestBody CyclePeriodRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return cyclePeriodService.create(request, email, nickname);
    }

    @PatchMapping("/{periodId}")
    public CyclePeriodResponse update(
            @PathVariable long periodId, @Valid @RequestBody CyclePeriodRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        return cyclePeriodService.update(periodId, request, email, nickname);
    }

    @DeleteMapping("/{periodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable long periodId,
            @RequestHeader(value = "X-User-Email", defaultValue = CurrentUserService.DEFAULT_EMAIL) String email,
            @RequestHeader(value = "X-User-Nickname", defaultValue = CurrentUserService.DEFAULT_NICKNAME) String nickname) {
        cyclePeriodService.delete(periodId, email, nickname);
    }
}
