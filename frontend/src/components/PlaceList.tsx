import { FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BoardMapType, Tag } from '../api/boardApi'
import { getApiErrorMessage } from '../api/client'
import {
  BoardPlace, RevisitIntention, deleteBoardPlace, deleteMyPlaceReview,
  fetchPlaceReviews, refreshBoardPlace, saveMyPlaceReview, updateBoardPlace,
} from '../api/placeApi'
import { Modal } from './Modal'
import { placeIcon, splitTagNames, tagIconMap, tagLabel } from './placeIcons'
import { directionsUrl } from './directions'

const REVISIT_LABELS: Record<RevisitIntention, string> = {
  YES: '○ 있음', MAYBE: '△ 고민', NO: '× 안 감',
}

const SOURCE_STATUS_MESSAGES: Record<Exclude<BoardPlace['sourceStatus'], 'ACTIVE'>, string> = {
  UNAVAILABLE: '지도 서비스에서 더 이상 이 장소 정보를 확인할 수 없어요.',
  MOVED: '원본 링크가 다른 장소로 이동했어요. 새 링크를 확인해 주세요.',
  CHECK_REQUIRED: '지도 서비스 연결 문제로 상태를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.',
}

function todayInputValue(): string {
  const today = new Date()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${today.getFullYear()}-${month}-${day}`
}

interface PlaceListProps {
  boardId: number
  places: BoardPlace[]
  tags: Tag[]
  isLoading: boolean
  filtered: boolean
  selectedId: number | null
  currentUserId?: number
  mapType: BoardMapType
  onSelect: (boardPlaceId: number | null) => void
}

export function PlaceList({ boardId, places, tags, isLoading, filtered, selectedId, currentUserId, mapType, onSelect }: PlaceListProps) {
  const queryClient = useQueryClient()
  const itemRefs = useRef(new Map<number, HTMLElement>())
  const icons = useMemo(() => tagIconMap(tags), [tags])
  const [editingPlaceId, setEditingPlaceId] = useState<number | null>(null)

  useEffect(() => {
    if (selectedId !== null) itemRefs.current.get(selectedId)?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [selectedId])

  if (isLoading) return <p className="empty">장소를 불러오는 중입니다.</p>
  if (places.length === 0 && filtered) return <p className="empty">조건에 맞는 장소가 없습니다.</p>
  if (places.length === 0) return <p className="empty">저장된 장소가 없습니다.<br />장소 추가로 지도 링크를 등록해 보세요.</p>

  return <ul className="place-list">{places.map((place) => {
    const selected = place.boardPlaceId === selectedId
    const tagNames = splitTagNames(place.tagNames)
    return <li key={place.boardPlaceId} ref={(element) => {
      if (element) itemRefs.current.set(place.boardPlaceId, element)
      else itemRefs.current.delete(place.boardPlaceId)
    }} className={selected ? 'place-item selected' : 'place-item'}>
      <div className="place-summary">
        <button type="button" className="place-summary-main" onClick={() => onSelect(selected ? null : place.boardPlaceId)}>
          <span className="place-icon" aria-hidden="true">{placeIcon(tagNames, icons, place.category)}</span>
          <span className="place-text">
            <span className="place-line" title={[place.name, place.category, ...tagNames.map((tag) => tagLabel(tag, icons.get(tag)))].filter(Boolean).join(' | ')}>
              <strong className="place-name">{place.name}</strong>
              {place.category && <span className="place-category">{place.category}</span>}
              {tagNames.length > 0 && <span className="place-tags">{tagNames.map((tag) => tagLabel(tag, icons.get(tag))).join(' ')}</span>}
            </span>
            <span className="place-address" title={place.address}>{place.latitude === null && <span className="hint">좌표 없음 · </span>}{place.address}</span>
          </span>
        </button>
        <span className="place-summary-actions">
          {place.revisitYesCount > 0 && <span className="revisit revisit-yes" title={`재방문 의사 있음 ${place.revisitYesCount}명`}>○ {place.revisitYesCount}</span>}
          {place.revisitMaybeCount > 0 && <span className="revisit revisit-maybe" title={`재방문 고민 ${place.revisitMaybeCount}명`}>△ {place.revisitMaybeCount}</span>}
          {place.revisitNoCount > 0 && <span className="revisit revisit-no" title={`재방문 의사 없음 ${place.revisitNoCount}명`}>× {place.revisitNoCount}</span>}
          {place.latitude !== null && place.longitude !== null && <a className="link-button place-directions" href={directionsUrl({
            latitude: place.latitude, longitude: place.longitude, name: place.name, mapType,
          })} target="_blank" rel="noreferrer">길찾기</a>}
          {selected && <button type="button" className="link-button place-summary-edit" onClick={() => setEditingPlaceId(place.boardPlaceId)}>수정</button>}
        </span>
      </div>
      {selected && <PlaceDetail boardId={boardId} place={place} currentUserId={currentUserId} onRemoved={() => onSelect(null)} />}
      {editingPlaceId === place.boardPlaceId && <PlaceEditModal boardId={boardId} place={place} tags={tags}
        onClose={() => setEditingPlaceId(null)} onSaved={() => queryClient.invalidateQueries({ queryKey: ['places', boardId] })} />}
    </li>
  })}</ul>
}

function PlaceDetail({ boardId, place, currentUserId, onRemoved }: {
  boardId: number
  place: BoardPlace
  currentUserId?: number
  onRemoved: () => void
}) {
  const queryClient = useQueryClient()
  const [revisitIntention, setRevisitIntention] = useState<RevisitIntention | ''>('')
  const [content, setContent] = useState('')
  const [visitedOn, setVisitedOn] = useState('')
  const [editingReview, setEditingReview] = useState(false)
  const refreshPlaces = () => queryClient.invalidateQueries({ queryKey: ['places', boardId] })
  const refreshReviews = () => queryClient.invalidateQueries({ queryKey: ['reviews', boardId, place.boardPlaceId] })
  const reviewsQuery = useQuery({ queryKey: ['reviews', boardId, place.boardPlaceId], queryFn: () => fetchPlaceReviews(boardId, place.boardPlaceId) })
  const myReview = reviewsQuery.data?.find((review) => review.userId === currentUserId)

  useEffect(() => {
    setRevisitIntention(myReview?.revisitIntention ?? '')
    setContent(myReview?.content ?? '')
    setVisitedOn(myReview?.visitedOn ?? '')
  }, [myReview])

  const deleteMutation = useMutation({ mutationFn: () => deleteBoardPlace(boardId, place.boardPlaceId), onSuccess: () => { onRemoved(); refreshPlaces() } })
  const reviewMutation = useMutation({
    mutationFn: () => saveMyPlaceReview(boardId, place.boardPlaceId, {
      revisitIntention: revisitIntention as RevisitIntention, content: content.trim(), visitedOn: visitedOn || null,
    }),
    onSuccess: () => { setEditingReview(false); refreshReviews(); refreshPlaces() },
  })
  const reviewDeleteMutation = useMutation({ mutationFn: () => deleteMyPlaceReview(boardId, place.boardPlaceId), onSuccess: () => { setEditingReview(false); refreshReviews(); refreshPlaces() } })
  const sourceRefreshMutation = useMutation({
    mutationFn: () => refreshBoardPlace(boardId, place.boardPlaceId),
    onSuccess: refreshPlaces,
  })
  const handleReviewSubmit = (event: FormEvent) => { event.preventDefault(); if (revisitIntention) reviewMutation.mutate() }
  const cancelReviewEdit = () => {
    setRevisitIntention(myReview?.revisitIntention ?? '')
    setContent(myReview?.content ?? '')
    setVisitedOn(myReview?.visitedOn ?? '')
    setEditingReview(false)
  }
  const error = deleteMutation.error || reviewMutation.error || reviewDeleteMutation.error
    || sourceRefreshMutation.error || reviewsQuery.error

  return <div className="place-detail">
    {place.sourceStatus !== 'ACTIVE' && <p className="notice">{SOURCE_STATUS_MESSAGES[place.sourceStatus]}</p>}
    {(reviewsQuery.data ?? []).length > 0 && <ul className="review-list">{(reviewsQuery.data ?? []).map((review) => <li key={review.reviewId}>
      <div className="review-heading">
        <strong>{review.nickname}</strong>
        <span className={`revisit revisit-${review.revisitIntention.toLowerCase()}`}>{REVISIT_LABELS[review.revisitIntention]}</span>
        {review.visitedOn && <time>{review.visitedOn}</time>}
        {review.userId === currentUserId && !editingReview && <button type="button" className="link-button review-edit" onClick={() => setEditingReview(true)}>수정</button>}
      </div>
      {review.content && <p>{review.content}</p>}
    </li>)}</ul>}
    {!myReview && !editingReview && <div className="review-create-action">
      <button type="button" className="link-button" onClick={() => setEditingReview(true)}>방문 기록 남기기</button>
    </div>}
    {editingReview && <form className="review-form" onSubmit={handleReviewSubmit}>
      <strong>{myReview ? '내 방문 기록 수정' : '내 방문 기록 작성'}</strong>
      <div className="revisit-options" role="group" aria-label="재방문 의사">
        {(Object.entries(REVISIT_LABELS) as [RevisitIntention, string][]).map(([value, label]) => <button type="button" key={value}
          className={revisitIntention === value ? `selected revisit-${value.toLowerCase()}` : ''} aria-pressed={revisitIntention === value}
          onClick={() => setRevisitIntention(value)}>{label}</button>)}
      </div>
      <textarea value={content} onChange={(event) => setContent(event.target.value)} rows={3} maxLength={1000} placeholder="어땠는지 남겨주세요 (선택)" />
      <label className="review-date">방문일 <input type="date" max={todayInputValue()} value={visitedOn} onChange={(event) => setVisitedOn(event.target.value)} /></label>
      <div className="review-actions">
        {myReview && <button type="button" className="link-button danger-text" disabled={reviewDeleteMutation.isPending} onClick={() => {
          if (window.confirm('내 방문 기록을 삭제할까요?')) reviewDeleteMutation.mutate()
        }}>기록 삭제</button>}
        <button type="button" className="link-button" onClick={cancelReviewEdit}>취소</button>
        <button className="small" disabled={!revisitIntention || reviewMutation.isPending}>{reviewMutation.isPending ? '저장 중' : '기록 저장'}</button>
      </div>
    </form>}
    <div className="detail-compose">
      {place.phone && <a className="detail-phone" href={`tel:${place.phone}`}>☎ {place.phone}</a>}
      <button type="button" className="link-button" disabled={sourceRefreshMutation.isPending}
        onClick={() => sourceRefreshMutation.mutate()}>
        {sourceRefreshMutation.isPending ? '확인 중' : '원본 정보 다시 확인'}
      </button>
      <button type="button" className="link-button danger-text" disabled={deleteMutation.isPending} onClick={() => {
        if (window.confirm(`${place.name}을(를) 보드에서 제외할까요?`)) deleteMutation.mutate()
      }}>제외</button>
    </div>
    {error && <p className="error-banner">{getApiErrorMessage(error)}</p>}
  </div>
}

function PlaceEditModal({ boardId, place, tags, onClose, onSaved }: {
  boardId: number
  place: BoardPlace
  tags: Tag[]
  onClose: () => void
  onSaved: () => void
}) {
  const assignedNames = splitTagNames(place.tagNames)
  const [tagIds, setTagIds] = useState(tags.filter((tag) => assignedNames.includes(tag.name)).map((tag) => tag.tagId))
  const saveMutation = useMutation({ mutationFn: () => updateBoardPlace(boardId, place.boardPlaceId, tagIds), onSuccess: () => { onSaved(); onClose() } })
  const toggleTag = (tagId: number) => setTagIds((current) => current.includes(tagId) ? current.filter((id) => id !== tagId) : [...current, tagId])

  return <Modal title={`${place.name} 수정`} onClose={onClose}>
    <fieldset className="field"><legend>태그</legend>
      {tags.length === 0 ? <p className="hint">태그 관리에서 태그를 먼저 만들어 주세요.</p> : <div className="tag-actions">{tags.map((tag) =>
        <button type="button" key={tag.tagId} className={tagIds.includes(tag.tagId) ? 'tag-button selected' : 'tag-button'}
          aria-pressed={tagIds.includes(tag.tagId)} onClick={() => toggleTag(tag.tagId)}>{tagLabel(tag.name, tag.icon)}</button>)}</div>}
    </fieldset>
    {saveMutation.error && <p className="error-banner">{getApiErrorMessage(saveMutation.error)}</p>}
    <div className="button-row modal-actions"><button type="button" className="secondary" onClick={onClose}>취소</button>
      <button disabled={saveMutation.isPending} onClick={() => saveMutation.mutate()}>{saveMutation.isPending ? '저장 중' : '저장'}</button></div>
  </Modal>
}
