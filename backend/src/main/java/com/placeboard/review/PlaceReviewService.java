package com.placeboard.review;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardService;
import com.placeboard.common.BusinessException;
import com.placeboard.place.BoardPlacesChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlaceReviewService {
    private final PlaceReviewMapper reviewMapper;
    private final BoardService boardService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    public PlaceReviewService(PlaceReviewMapper reviewMapper, BoardService boardService,
                              CurrentUserService currentUserService, ApplicationEventPublisher eventPublisher) {
        this.reviewMapper = reviewMapper;
        this.boardService = boardService;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public List<PlaceReviewResponse> findAll(long boardId, long boardPlaceId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        return reviewMapper.findAll(boardId, boardPlaceId);
    }

    @Transactional
    public PlaceReviewResponse saveMine(long boardId, long boardPlaceId, PlaceReviewRequest request,
                                        String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        String content = request.content() == null || request.content().isBlank() ? null : request.content().trim();
        if (!reviewMapper.placeExists(boardId, boardPlaceId)) throw notFound();
        reviewMapper.upsert(boardId, boardPlaceId, user.userId(), request.revisitIntention(),
                content, request.visitedOn());
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
        return reviewMapper.findMine(boardId, boardPlaceId, user.userId());
    }

    @Transactional
    public void deleteMine(long boardId, long boardPlaceId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        if (reviewMapper.deleteMine(boardId, boardPlaceId, user.userId()) == 0) throw notFound();
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
    }

    private BusinessException notFound() {
        return new BusinessException("PLACE_REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND,
                "장소 또는 방문 기록을 찾을 수 없습니다.");
    }
}
