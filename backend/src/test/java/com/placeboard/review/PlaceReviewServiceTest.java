package com.placeboard.review;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardService;
import com.placeboard.place.BoardPlacesChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceReviewServiceTest {
    private final PlaceReviewMapper reviewMapper = mock(PlaceReviewMapper.class);
    private final BoardService boardService = mock(BoardService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private PlaceReviewService service;

    @BeforeEach
    void setUp() {
        service = new PlaceReviewService(reviewMapper, boardService, currentUserService, eventPublisher);
        when(currentUserService.resolve("user@example.com", "사용자"))
                .thenReturn(new UserSummary(7L, "user@example.com", "사용자"));
        when(reviewMapper.placeExists(1L, 2L)).thenReturn(true);
    }

    @Test
    void savesOneReviewForCurrentMemberAndTrimsContent() {
        LocalDate visitedOn = LocalDate.of(2026, 10, 8);
        PlaceReviewRequest request = new PlaceReviewRequest(RevisitIntention.YES, "  또 가고 싶어요  ", visitedOn);
        PlaceReviewResponse expected = new PlaceReviewResponse(3L, 2L, 7L, "사용자",
                RevisitIntention.YES, "또 가고 싶어요", visitedOn, null, null);
        when(reviewMapper.findMine(1L, 2L, 7L)).thenReturn(expected);

        PlaceReviewResponse result = service.saveMine(1L, 2L, request, "user@example.com", "사용자");

        assertThat(result).isEqualTo(expected);
        verify(boardService).requireAccess(1L, 7L);
        verify(reviewMapper).upsert(1L, 2L, 7L, RevisitIntention.YES, "또 가고 싶어요", visitedOn);
        verify(eventPublisher).publishEvent(new BoardPlacesChangedEvent(1L, 7L));
    }
}
