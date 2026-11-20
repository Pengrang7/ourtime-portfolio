package com.placeboard.place;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardAccess;
import com.placeboard.board.BoardService;
import com.placeboard.common.BusinessException;
import com.placeboard.common.MapProviderConnectionException;
import com.placeboard.review.RevisitFilter;
import com.placeboard.tag.TagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class BoardPlaceService {
    private static final Logger log = LoggerFactory.getLogger(BoardPlaceService.class);
    private final PlaceMapper placeMapper;
    private final BoardService boardService;
    private final CurrentUserService currentUserService;
    private final TagMapper tagMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PlacePreviewService placePreviewService;

    public BoardPlaceService(PlaceMapper placeMapper, BoardService boardService,
                             CurrentUserService currentUserService, TagMapper tagMapper,
                             ApplicationEventPublisher eventPublisher, PlacePreviewService placePreviewService) {
        this.placeMapper = placeMapper;
        this.boardService = boardService;
        this.currentUserService = currentUserService;
        this.tagMapper = tagMapper;
        this.eventPublisher = eventPublisher;
        this.placePreviewService = placePreviewService;
    }

    @Transactional
    public PlaceListItem save(long boardId, PlaceSaveRequest request, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());

        String providerPlaceId = normalizeNullable(request.providerPlaceId());
        Long placeId = providerPlaceId == null ? null
                : placeMapper.findPlaceIdBySource(request.provider().name(), providerPlaceId);

        if (placeId == null) {
            placeMapper.insertPlace(request, user.userId());
            placeId = placeMapper.lastInsertId();
            placeMapper.insertSource(placeId, request);
        } else {
            placeMapper.updatePlaceFromSaveRequest(placeId, request, user.userId());
            placeMapper.reactivatePlace(placeId);
            placeMapper.reactivateSource(request);
        }

        Long boardPlaceId = placeMapper.findBoardPlaceId(boardId, placeId);
        if (boardPlaceId == null) {
            placeMapper.insertBoardPlace(boardId, placeId, user.userId());
            boardPlaceId = placeMapper.lastInsertId();
        } else {
            placeMapper.reactivateBoardPlace(boardPlaceId);
        }

        if (request.tagIds() != null) {
            for (Long tagId : new LinkedHashSet<>(request.tagIds())) {
                if (tagMapper.assign(boardId, boardPlaceId, tagId, user.userId()) == 0) {
                    throw new BusinessException("TAG_NOT_FOUND", HttpStatus.NOT_FOUND, "태그를 찾을 수 없습니다.");
                }
            }
        }
        PlaceListItem saved = placeMapper.findOne(boardId, boardPlaceId);
        if (saved == null) throw notFound();
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
        return saved;
    }

    @Transactional
    public PlaceListItem refresh(long boardId, long boardPlaceId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = boardService.requireAccess(boardId, user.userId());
        if (!access.canEditPlace()) {
            throw new BusinessException("PLACE_EDIT_FORBIDDEN", HttpStatus.FORBIDDEN, "장소 원본 정보 수정 권한이 없습니다.");
        }
        PlaceRefreshSource source = placeMapper.findRefreshSource(boardId, boardPlaceId);
        if (source == null) throw notFound();

        try {
            PlacePreviewResponse preview = placePreviewService.preview(source.originalUrl());
            boolean sameSource = source.provider().equals(preview.provider().name())
                    && Objects.equals(source.providerPlaceId(), preview.providerPlaceId());
            if (!sameSource) {
                placeMapper.updateSourceStatus(source.placeId(), source.provider(), PlaceSourceStatus.MOVED);
            } else {
                placeMapper.updatePlaceFromPreview(source.placeId(), preview, user.userId());
                placeMapper.updateSourceStatus(source.placeId(), source.provider(), PlaceSourceStatus.ACTIVE);
            }
        } catch (PlaceUnavailableException exception) {
            log.info("장소 원본 정보 조회 불가: boardId={}, boardPlaceId={}, provider={}",
                    boardId, boardPlaceId, source.provider());
            placeMapper.updateSourceStatus(source.placeId(), source.provider(), PlaceSourceStatus.UNAVAILABLE);
        } catch (MapProviderConnectionException | UnsupportedMapUrlException exception) {
            log.warn("장소 원본 정보 재확인 실패: boardId={}, boardPlaceId={}, provider={}",
                    boardId, boardPlaceId, source.provider(), exception);
            placeMapper.updateSourceStatus(source.placeId(), source.provider(), PlaceSourceStatus.CHECK_REQUIRED);
        }

        PlaceListItem refreshed = placeMapper.findOne(boardId, boardPlaceId);
        if (refreshed == null) throw notFound();
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
        return refreshed;
    }

    public List<PlaceListItem> findAll(long boardId, RevisitFilter revisit, String sido, String sigungu,
                                       Long tagId, String keyword,
                                       String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        return placeMapper.findAll(boardId, revisit, normalizeNullable(sido),
                normalizeNullable(sigungu), tagId, escapeLike(normalizeNullable(keyword)));
    }

    public List<PlaceRegionOption> findRegionOptions(long boardId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        return placeMapper.findRegionOptions(boardId);
    }

    // 사용자가 입력한 %, _ 는 와일드카드가 아닌 글자 그대로 찾는다.
    private String escapeLike(String value) {
        return value == null ? null : value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional
    public void update(long boardId, long boardPlaceId, BoardPlaceUpdateRequest request,
                       String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        boardService.requireAccess(boardId, user.userId());
        if (!placeMapper.exists(boardId, boardPlaceId)) throw notFound();

        tagMapper.clearAssignments(boardId, boardPlaceId);
        if (request.tagIds() != null) {
            for (Long tagId : new LinkedHashSet<>(request.tagIds())) {
                if (tagMapper.assign(boardId, boardPlaceId, tagId, user.userId()) == 0) {
                    throw new BusinessException("TAG_NOT_FOUND", HttpStatus.NOT_FOUND, "태그를 찾을 수 없습니다.");
                }
            }
        }
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
    }

    @Transactional
    public void delete(long boardId, long boardPlaceId, String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = boardService.requireAccess(boardId, user.userId());
        if (!access.canDeletePlace()) {
            throw new BusinessException("PLACE_DELETE_FORBIDDEN", HttpStatus.FORBIDDEN, "장소 삭제 권한이 없습니다.");
        }
        if (placeMapper.softDelete(boardId, boardPlaceId) == 0) throw notFound();
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
    }

    @Transactional
    public void updatePlace(long boardId, long placeId, PlaceUpdateRequest request,
                            String email, String nickname) {
        UserSummary user = currentUserService.resolve(email, nickname);
        BoardAccess access = boardService.requireAccess(boardId, user.userId());
        if (!access.canEditPlace()) {
            throw new BusinessException("PLACE_EDIT_FORBIDDEN", HttpStatus.FORBIDDEN, "장소 원본 정보 수정 권한이 없습니다.");
        }
        if (placeMapper.updateBoardPlaceDetails(boardId, placeId, request) == 0) {
            throw notFound();
        }
        eventPublisher.publishEvent(new BoardPlacesChangedEvent(boardId, user.userId()));
    }

    private BusinessException notFound() {
        return new BusinessException("PLACE_NOT_FOUND", HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
