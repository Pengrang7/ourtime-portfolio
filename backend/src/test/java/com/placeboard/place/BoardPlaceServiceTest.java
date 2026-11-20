package com.placeboard.place;

import com.placeboard.auth.CurrentUserService;
import com.placeboard.auth.UserSummary;
import com.placeboard.board.BoardAccess;
import com.placeboard.board.BoardService;
import com.placeboard.tag.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class BoardPlaceServiceTest {
    private final PlaceMapper placeMapper = mock(PlaceMapper.class);
    private final BoardService boardService = mock(BoardService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final TagMapper tagMapper = mock(TagMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final PlacePreviewService placePreviewService = mock(PlacePreviewService.class);
    private BoardPlaceService boardPlaceService;

    @BeforeEach
    void setUp() {
        boardPlaceService = new BoardPlaceService(
                placeMapper, boardService, currentUserService, tagMapper, eventPublisher, placePreviewService);
        when(currentUserService.resolve("user@example.com", "사용자"))
                .thenReturn(new UserSummary(7L, "user@example.com", "사용자"));
        when(boardService.requireAccess(1L, 7L))
                .thenReturn(new BoardAccess(1L, 2L, false, false, false, false));
    }

    @Test
    void updatesTagsAsOneServiceOperation() {
        BoardPlaceUpdateRequest request = new BoardPlaceUpdateRequest(List.of(3L, 3L, 4L));
        when(placeMapper.exists(1L, 2L)).thenReturn(true);
        when(tagMapper.assign(1L, 2L, 3L, 7L)).thenReturn(1);
        when(tagMapper.assign(1L, 2L, 4L, 7L)).thenReturn(1);

        boardPlaceService.update(1L, 2L, request, "user@example.com", "사용자");

        verify(tagMapper).clearAssignments(1L, 2L);
        verify(tagMapper, times(1)).assign(1L, 2L, 3L, 7L);
        verify(tagMapper, times(1)).assign(1L, 2L, 4L, 7L);
        verify(eventPublisher).publishEvent(new BoardPlacesChangedEvent(1L, 7L));
    }

    @Test
    void returnsAllRegionOptionsIndependentlyFromPlaceFilters() {
        List<PlaceRegionOption> expected = List.of(
                new PlaceRegionOption("경기도", "수원시"),
                new PlaceRegionOption("경상북도", "경주시"));
        when(placeMapper.findRegionOptions(1L)).thenReturn(expected);

        List<PlaceRegionOption> regions = boardPlaceService.findRegionOptions(1L, "user@example.com", "사용자");

        assertThat(regions).containsExactlyElementsOf(expected);
        verify(boardService).requireAccess(1L, 7L);
        verify(placeMapper).findRegionOptions(1L);
    }

    @Test
    void refreshesPlaceWhenProviderPlaceIdIsUnchanged() {
        when(boardService.requireAccess(1L, 7L))
                .thenReturn(new BoardAccess(1L, 2L, true, false, false, false));
        PlaceRefreshSource source = new PlaceRefreshSource(3L, "NAVER", "12345", "https://naver.me/test");
        PlacePreviewResponse preview = PlacePreviewResponse.of(
                com.placeboard.map.MapProviderType.NAVER, "12345", source.originalUrl(), "새 이름",
                "서울 강남구 테헤란로 1", null, new BigDecimal("37.1"),
                new BigDecimal("127.1"), "카페", null);
        when(placeMapper.findRefreshSource(1L, 2L)).thenReturn(source);
        when(placePreviewService.preview(source.originalUrl())).thenReturn(preview);
        PlaceListItem refreshed = mock(PlaceListItem.class);
        when(placeMapper.findOne(1L, 2L)).thenReturn(refreshed);

        assertThat(boardPlaceService.refresh(1L, 2L, "user@example.com", "사용자")).isSameAs(refreshed);

        verify(placeMapper).updatePlaceFromPreview(3L, preview, 7L);
        verify(placeMapper).updateSourceStatus(3L, "NAVER", PlaceSourceStatus.ACTIVE);
    }

    @Test
    void keepsPlaceAndMarksUnavailableWhenProviderRemovedIt() {
        when(boardService.requireAccess(1L, 7L))
                .thenReturn(new BoardAccess(1L, 2L, true, false, false, false));
        PlaceRefreshSource source = new PlaceRefreshSource(3L, "NAVER", "12345", "https://naver.me/test");
        when(placeMapper.findRefreshSource(1L, 2L)).thenReturn(source);
        when(placePreviewService.preview(source.originalUrl()))
                .thenThrow(new PlaceUnavailableException("장소 없음"));
        PlaceListItem refreshed = mock(PlaceListItem.class);
        when(placeMapper.findOne(1L, 2L)).thenReturn(refreshed);

        assertThat(boardPlaceService.refresh(1L, 2L, "user@example.com", "사용자")).isSameAs(refreshed);

        verify(placeMapper).updateSourceStatus(3L, "NAVER", PlaceSourceStatus.UNAVAILABLE);
    }
}
