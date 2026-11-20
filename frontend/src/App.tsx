import { CSSProperties, KeyboardEvent, PointerEvent, useEffect, useMemo, useRef, useState } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { CurrentUser } from './api/authApi'
import { Tag, fetchBoards, fetchMembers, fetchTags } from './api/boardApi'
import { getApiErrorMessage } from './api/client'
import { BoardPlace, RevisitFilter, fetchPlaceRegions, fetchPlaces } from './api/placeApi'
import { AddPlaceModal } from './components/AddPlaceModal'
import { BoardSwitcher } from './components/BoardSwitcher'
import { InviteMemberModal } from './components/InviteMemberModal'
import { PlaceMap } from './components/PlaceMap'
import { placeIcon, splitTagNames, tagIconMap, tagLabel } from './components/placeIcons'
import { PlaceList } from './components/PlaceList'
import { SchedulePlanner } from './components/SchedulePlanner'
import { TagManageModal } from './components/TagManageModal'
import { useBoardEvents } from './hooks/useBoardEvents'
import { searchMapLocation } from './components/mapLocationSearch'

type ModalType = 'addPlace' | 'invite' | 'tags' | null
type WorkspaceMode = 'places' | 'schedule'

const LIST_WIDTH_KEY = 'placeBoard.listWidth'
const DEFAULT_LIST_WIDTH = 420
const MIN_LIST_WIDTH = 260
const MIN_MAP_WIDTH = 240
const EMPTY_PLACES: BoardPlace[] = []
const EMPTY_TAGS: Tag[] = []

const SIDO_LABELS: Record<string, string> = {
  서울특별시: '서울', 부산광역시: '부산', 대구광역시: '대구', 인천광역시: '인천',
  광주광역시: '광주', 대전광역시: '대전', 울산광역시: '울산', 세종특별자치시: '세종',
  경기도: '경기', 강원특별자치도: '강원', 강원도: '강원', 충청북도: '충북', 충청남도: '충남',
  전북특별자치도: '전북', 전라북도: '전북', 전라남도: '전남', 경상북도: '경북',
  경상남도: '경남', 제주특별자치도: '제주',
}

function sidoLabel(sido: string): string {
  return SIDO_LABELS[sido] ?? sido.replace(/(특별자치도|특별자치시|특별시|광역시|도)$/, '')
}

function sigunguLabel(sigungu: string): string {
  return sigungu.replace(/(시|군|구)$/, '')
}

function readListWidth(): number {
  const saved = Number(localStorage.getItem(LIST_WIDTH_KEY))
  return Number.isFinite(saved) && saved >= MIN_LIST_WIDTH ? saved : DEFAULT_LIST_WIDTH
}

interface AppProps {
  me: CurrentUser & { nickname: string }
  onLogout: () => void
}

function App({ me, onLogout }: AppProps) {
  const queryClient = useQueryClient()
  const [selectedBoardId, setSelectedBoardId] = useState<number | null>(null)
  const [revisitFilter, setRevisitFilter] = useState<RevisitFilter | ''>('')
  const [sidoFilter, setSidoFilter] = useState('')
  const [sigunguFilter, setSigunguFilter] = useState('')
  const [tagFilter, setTagFilter] = useState<number | ''>('')
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [mapSearchKeyword, setMapSearchKeyword] = useState('')
  const [mapSearchRequest, setMapSearchRequest] = useState(0)
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null)
  const [modal, setModal] = useState<ModalType>(null)
  const [workspaceMode, setWorkspaceMode] = useState<WorkspaceMode>('places')
  const [listWidth, setListWidth] = useState(readListWidth)
  const boardViewRef = useRef<HTMLDivElement>(null)

  useBoardEvents(selectedBoardId)

  const boardsQuery = useQuery({ queryKey: ['boards'], queryFn: fetchBoards })
  useEffect(() => {
    if (boardsQuery.data?.length && !boardsQuery.data.some((board) => board.boardId === selectedBoardId)) {
      setSelectedBoardId(boardsQuery.data[0].boardId)
    }
    if (boardsQuery.data?.length === 0) setSelectedBoardId(null)
  }, [boardsQuery.data, selectedBoardId])
  useEffect(() => setSelectedPlaceId(null), [selectedBoardId])
  useEffect(() => {
    setRevisitFilter('')
    setSidoFilter('')
    setSigunguFilter('')
    setTagFilter('')
    setKeywordInput('')
    setKeyword('')
    setMapSearchKeyword('')
  }, [selectedBoardId])
  // 타이핑마다 요청하지 않도록 입력이 잠시 멈추면 검색한다.
  useEffect(() => {
    const timer = window.setTimeout(() => setKeyword(keywordInput.trim()), 250)
    return () => window.clearTimeout(timer)
  }, [keywordInput])

  const placesQuery = useQuery({
    queryKey: ['places', selectedBoardId, revisitFilter, sidoFilter, sigunguFilter, tagFilter, keyword],
    queryFn: () => fetchPlaces(selectedBoardId!, {
      revisit: revisitFilter || undefined,
      sido: sidoFilter || undefined,
      sigungu: sigunguFilter || undefined,
      tagId: tagFilter || undefined,
      keyword: keyword || undefined,
    }),
    enabled: selectedBoardId !== null,
    placeholderData: keepPreviousData,
  })
  const regionsQuery = useQuery({
    queryKey: ['place-regions', selectedBoardId],
    queryFn: () => fetchPlaceRegions(selectedBoardId!),
    enabled: selectedBoardId !== null,
  })
  const tagsQuery = useQuery({
    queryKey: ['tags', selectedBoardId],
    queryFn: () => fetchTags(selectedBoardId!),
    enabled: selectedBoardId !== null,
  })
  const membersQuery = useQuery({
    queryKey: ['members', selectedBoardId],
    queryFn: () => fetchMembers(selectedBoardId!),
    enabled: selectedBoardId !== null,
  })
  const googleMapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY?.trim() ?? ''
  const mapLocationQuery = useQuery({
    queryKey: ['map-location-search', mapSearchKeyword],
    queryFn: () => searchMapLocation(mapSearchKeyword, googleMapsApiKey),
    enabled: Boolean(mapSearchKeyword && googleMapsApiKey),
    staleTime: 24 * 60 * 60 * 1000,
    retry: false,
  })
  const mapSearchTarget = useMemo(() => mapLocationQuery.data
    ? { ...mapLocationQuery.data, request: mapSearchRequest }
    : null, [mapLocationQuery.data, mapSearchRequest])

  const submitMapSearch = () => {
    const nextKeyword = keywordInput.trim()
    if (!nextKeyword) return
    setKeyword(nextKeyword)
    if (nextKeyword === mapSearchKeyword && mapLocationQuery.isError) {
      void mapLocationQuery.refetch()
    }
    setMapSearchKeyword(nextKeyword)
    setMapSearchRequest((current) => current + 1)
    setSelectedPlaceId(null)
  }

  const places = placesQuery.data ?? EMPTY_PLACES
  const tags = tagsQuery.data ?? EMPTY_TAGS
  const icons = useMemo(() => tagIconMap(tags), [tags])
  const mapPlaces = useMemo(() => places
    .filter((place) => place.latitude !== null && place.longitude !== null)
    .map((place) => ({
      id: place.boardPlaceId,
      name: place.name,
      icon: placeIcon(splitTagNames(place.tagNames), icons, place.category),
      latitude: place.latitude!,
      longitude: place.longitude!,
      country: place.country,
      address: place.address,
      category: place.category,
      phone: place.phone,
      tagNames: place.tagNames,
      provider: place.provider,
      originalUrl: place.originalUrl,
    })), [places, icons])
  const regionOptions = useMemo(() => regionsQuery.data ?? [], [regionsQuery.data])
  const sidoOptions = useMemo(
    () => [...new Set(regionOptions.map((region) => region.sido))],
    [regionOptions],
  )
  const sigunguOptions = useMemo(
    () => [...new Set(regionOptions
      .filter((region) => region.sido === sidoFilter)
      .map((region) => region.sigungu)
      .filter((sigungu): sigungu is string => Boolean(sigungu)))],
    [regionOptions, sidoFilter],
  )
  const currentUserId = me.userId
  const currentBoard = boardsQuery.data?.find((board) => board.boardId === selectedBoardId)
  const currentMember = membersQuery.data?.find((member) => member.userId === currentUserId)
  const canManageMembers = currentMember?.canInviteMember ?? false

  const clampListWidth = (width: number) => {
    const boardWidth = boardViewRef.current?.getBoundingClientRect().width ?? Infinity
    return Math.round(Math.min(Math.max(width, MIN_LIST_WIDTH), boardWidth - MIN_MAP_WIDTH))
  }
  const commitListWidth = (width: number) => {
    const next = clampListWidth(width)
    setListWidth(next)
    localStorage.setItem(LIST_WIDTH_KEY, String(next))
  }
  const startResize = (event: PointerEvent<HTMLDivElement>) => {
    const boardLeft = boardViewRef.current?.getBoundingClientRect().left ?? 0
    const handle = event.currentTarget
    handle.setPointerCapture(event.pointerId)
    document.body.classList.add('resizing')
    const onMove = (moveEvent: globalThis.PointerEvent) => setListWidth(clampListWidth(moveEvent.clientX - boardLeft))
    const onUp = (upEvent: globalThis.PointerEvent) => {
      handle.removeEventListener('pointermove', onMove)
      handle.removeEventListener('pointerup', onUp)
      handle.removeEventListener('pointercancel', onUp)
      document.body.classList.remove('resizing')
      commitListWidth(upEvent.clientX - boardLeft)
    }
    handle.addEventListener('pointermove', onMove)
    handle.addEventListener('pointerup', onUp)
    handle.addEventListener('pointercancel', onUp)
  }
  const resizeByKey = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'ArrowLeft') commitListWidth(listWidth - 20)
    else if (event.key === 'ArrowRight') commitListWidth(listWidth + 20)
    else return
    event.preventDefault()
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <h1 className="brand" aria-label="우리 둘의 장소 보드">
          <span className="brand-mark" aria-hidden="true" />
          <span>우리의 장소</span>
        </h1>
        <BoardSwitcher
          boards={boardsQuery.data ?? []}
          currentBoard={currentBoard}
          canRename={currentBoard !== undefined && currentBoard.ownerId === currentUserId}
          onSelect={setSelectedBoardId}
        />
        {selectedBoardId && (
          <div className="toolbar">
            <button className="secondary" onClick={() => setWorkspaceMode((current) => current === 'places' ? 'schedule' : 'places')}>
              {workspaceMode === 'places' ? '📅 데이트 일정' : '🗺 장소 모아보기'}
            </button>
            <button onClick={() => setModal('addPlace')}>＋ 장소 추가</button>
            <button className="secondary" onClick={() => setModal('invite')}>멤버</button>
            <button className="secondary" onClick={() => setModal('tags')}>태그 관리</button>
          </div>
        )}
        <div className="user-chip" title={`${me.nickname} · ${me.email}`}>
          <strong>{me.nickname}</strong>
          <span>{me.email}</span>
        </div>
        <button type="button" className="logout-button" onClick={onLogout}>로그아웃</button>
      </header>

      <section className="workspace">
        {!selectedBoardId ? (
          <div className="empty-state">
            <div className="empty-state-icon" aria-hidden="true" />
            <h2>우리만의 보드를 만들어 볼까요?</h2>
            <p>위쪽 보드 메뉴(▾)에서 이름을 정하고, 함께 가고 싶은 장소와 추억을 차곡차곡 모아보세요.</p>
          </div>
        ) : (
          <>
            {workspaceMode === 'schedule' ? (
              <SchedulePlanner
                boardId={selectedBoardId}
                currentUserId={currentUserId}
                boardOwnerId={currentBoard?.ownerId ?? 0}
                mapType={currentBoard?.mapType ?? 'DOMESTIC'}
                tags={tags}
              />
            ) : (
              <>
                {placesQuery.error && <p className="error-banner">{getApiErrorMessage(placesQuery.error)}</p>}

                <div className="board-view" ref={boardViewRef} style={{ '--list-width': `${listWidth}px` } as CSSProperties}>
              <section className="list-pane" aria-label="장소 목록">
                <div className="filters">
                  <form className="place-search-form" onSubmit={(event) => {
                    event.preventDefault()
                    submitMapSearch()
                  }}>
                    <input
                      type="search"
                      className="place-search"
                      value={keywordInput}
                      onChange={(event) => {
                        const value = event.target.value
                        setKeywordInput(value)
                        if (!value.trim()) setMapSearchKeyword('')
                      }}
                      placeholder="🔍 장소·역·지역 검색"
                      aria-label="장소 검색"
                      maxLength={100}
                    />
                    <button type="submit" className="small" disabled={!keywordInput.trim() || mapLocationQuery.isFetching}>
                      {mapLocationQuery.isFetching ? '검색 중' : '지도 이동'}
                    </button>
                  </form>
                  <select value={revisitFilter} onChange={(event) => setRevisitFilter(event.target.value as RevisitFilter | '')} aria-label="재방문 필터">
                    <option value="">재방문 전체</option>
                    <option value="YES">○ 있음</option>
                    <option value="MAYBE">△ 고민</option>
                    <option value="NO">× 안 감</option>
                    <option value="UNREVIEWED">방문 기록 없음</option>
                  </select>
                  <select value={sidoFilter} onChange={(event) => {
                    setSidoFilter(event.target.value)
                    setSigunguFilter('')
                  }} aria-label="시도 필터">
                    <option value="">전체 시도</option>
                    {sidoOptions.map((sido) => <option key={sido} value={sido}>{sidoLabel(sido)}</option>)}
                  </select>
                  <select
                    value={sigunguFilter}
                    onChange={(event) => setSigunguFilter(event.target.value)}
                    aria-label="시군구 필터"
                    disabled={!sidoFilter}
                  >
                    <option value="">전체 시군구</option>
                    {sigunguOptions.map((sigungu) => (
                      <option key={sigungu} value={sigungu}>{sigunguLabel(sigungu)}</option>
                    ))}
                  </select>
                  <select value={tagFilter} onChange={(event) => setTagFilter(event.target.value ? Number(event.target.value) : '')} aria-label="태그 필터">
                    <option value="">전체 태그</option>
                    {tags.map((tag) => <option key={tag.tagId} value={tag.tagId}>{tagLabel(tag.name, tag.icon)}</option>)}
                  </select>
                </div>
                <p className="list-count">장소 {places.length}곳</p>
                <PlaceList
                  boardId={selectedBoardId}
                  places={places}
                  tags={tags}
                  isLoading={placesQuery.isLoading}
                  filtered={Boolean(revisitFilter || sidoFilter || sigunguFilter || tagFilter || keyword)}
                  selectedId={selectedPlaceId}
                  currentUserId={currentUserId}
                  mapType={currentBoard?.mapType ?? 'DOMESTIC'}
                  onSelect={setSelectedPlaceId}
                />
              </section>

              <div
                className="resize-handle"
                role="separator"
                aria-orientation="vertical"
                aria-label="목록 너비 조절"
                aria-valuenow={listWidth}
                tabIndex={0}
                title="드래그해서 목록 너비 조절 · 더블클릭하면 기본값"
                onPointerDown={startResize}
                onKeyDown={resizeByKey}
                onDoubleClick={() => commitListWidth(DEFAULT_LIST_WIDTH)}
              />

              <section className="map-pane" aria-label="지도">
                <PlaceMap
                  places={mapPlaces}
                  selectedId={selectedPlaceId}
                  onSelect={setSelectedPlaceId}
                  mapType={currentBoard?.mapType}
                  searchTarget={mapSearchKeyword ? mapSearchTarget : null}
                />
              </section>
                </div>
              </>
            )}

            {modal === 'addPlace' && (
              <AddPlaceModal
                boardId={selectedBoardId}
                tags={tags}
                onClose={() => setModal(null)}
                onSaved={(place) => {
                  setModal(null)
                  queryClient.invalidateQueries({ queryKey: ['places', selectedBoardId] })
                  setSelectedPlaceId(place.boardPlaceId)
                }}
              />
            )}
            {modal === 'invite' && (
              <InviteMemberModal
                boardId={selectedBoardId}
                members={membersQuery.data ?? []}
                currentUserId={currentUserId}
                canManageMembers={canManageMembers}
                onClose={() => setModal(null)}
              />
            )}
            {modal === 'tags' && (
              <TagManageModal
                boardId={selectedBoardId}
                tags={tags}
                onClose={() => setModal(null)}
                onDeleted={(tagId) => {
                  if (tagFilter === tagId) setTagFilter('')
                }}
              />
            )}
          </>
        )}
      </section>
    </main>
  )
}

export default App
