import { CSSProperties, FormEvent, KeyboardEvent, useEffect, useMemo, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Tag } from '../api/boardApi'
import { BoardPlace, RevisitFilter } from '../api/placeApi'
import { getApiErrorMessage } from '../api/client'
import { SCHEDULE_COLORS, SCHEDULE_COLOR_LABELS, Schedule, SchedulePayload, createSchedule, deleteSchedule, updateSchedule } from '../api/scheduleApi'
import { Modal } from './Modal'
import { tagLabel } from './placeIcons'
import { allDayLastDate, allDayRange } from './scheduleCalendar'
import { filterPlaces, placeRegion } from './placeSearch'

interface ScheduleModalProps {
  boardId: number
  places: BoardPlace[]
  tags: Tag[]
  initialStart: Date
  initialAllDay?: boolean
  schedule: Schedule | null
  currentUserId: number
  boardOwnerId: number
  onClose: () => void
  onSaved: () => void
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function toInputValue(value: Date | string): string {
  const date = new Date(value)
  date.setMinutes(Math.round(date.getMinutes() / 10) * 10, 0, 0)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 태그를 붙이면 자동으로 정해지는 일정 색 (Google Calendar 색 이름 기준). */
const TAG_COLORS: Record<string, string> = {
  데이트: '#E67C73', // 플라밍고
}

const HOURS = Array.from({ length: 24 }, (_, index) => index)
const MINUTES = Array.from({ length: 6 }, (_, index) => index * 10)

function dateTimeParts(value: string) {
  const [date, time = '00:00'] = value.split('T')
  const [hour = '0', minute = '0'] = time.split(':')
  return { date, hour: Number(hour), minute: Number(minute) }
}

function updateDateTime(value: string, next: Partial<ReturnType<typeof dateTimeParts>>) {
  const current = dateTimeParts(value)
  const merged = { ...current, ...next }
  return `${merged.date}T${pad(merged.hour)}:${pad(merged.minute)}`
}

function ScheduleDateTimeField({
  label,
  value,
  onChange,
  disabled,
  minDate,
  dateOnly = false,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  disabled: boolean
  minDate?: string
  dateOnly?: boolean
}) {
  const parts = dateTimeParts(value)

  return (
    <div className="schedule-date-time-field">
      <strong>{label}</strong>
      <div className={`schedule-date-time-controls ${dateOnly ? 'date-only' : ''}`}>
        <input
          type="date"
          aria-label={`${label} 날짜`}
          value={parts.date}
          min={minDate}
          onChange={(event) => onChange(updateDateTime(value, { date: event.target.value }))}
          disabled={disabled}
          required
        />
        {!dateOnly && <>
        <select
          aria-label={`${label} 시`}
          value={parts.hour}
          onChange={(event) => onChange(updateDateTime(value, { hour: Number(event.target.value) }))}
          disabled={disabled}
        >
          {HOURS.map((hour) => <option key={hour} value={hour}>{hour}시</option>)}
        </select>
        <select
          aria-label={`${label} 분`}
          value={parts.minute}
          onChange={(event) => onChange(updateDateTime(value, { minute: Number(event.target.value) }))}
          disabled={disabled}
        >
          {MINUTES.map((minute) => <option key={minute} value={minute}>{pad(minute)}분</option>)}
        </select>
        </>}
      </div>
    </div>
  )
}

export function SchedulePlaceSearch({ places, tags, value, onChange, disabled }: {
  places: BoardPlace[]
  tags: Tag[]
  value: number | null
  onChange: (boardPlaceId: number | null) => void
  disabled: boolean
}) {
  const selected = places.find((place) => place.boardPlaceId === value) ?? null
  const [keyword, setKeyword] = useState('')
  const [regionFilter, setRegionFilter] = useState('')
  const [tagFilter, setTagFilter] = useState('')
  const [revisitFilter, setRevisitFilter] = useState<RevisitFilter | ''>('')
  const [open, setOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(0)
  const rootRef = useRef<HTMLDivElement>(null)
  const regionOptions = useMemo(() => [...new Set(places.flatMap((place) => {
    if (!place.sido) return []
    return place.sigungu ? [place.sido, `${place.sido} ${place.sigungu}`] : [place.sido]
  }))], [places])
  const results = useMemo(
    () => filterPlaces(places, keyword, { region: regionFilter, tag: tagFilter, revisit: revisitFilter || undefined }),
    [places, keyword, regionFilter, tagFilter, revisitFilter],
  )

  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  const choose = (place: BoardPlace | null) => {
    onChange(place?.boardPlaceId ?? null)
    setKeyword('')
    setOpen(false)
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      setOpen(true)
      const step = event.key === 'ArrowDown' ? 1 : -1
      setActiveIndex((index) => Math.min(Math.max(index + step, 0), Math.max(results.length - 1, 0)))
    } else if (event.key === 'Enter' && open) {
      // 목록이 열려 있을 때 Enter 는 폼 저장이 아니라 장소 선택으로 처리한다.
      event.preventDefault()
      if (results[activeIndex]) choose(results[activeIndex])
    } else if (event.key === 'Escape' && open) {
      event.stopPropagation()
      setOpen(false)
    }
  }

  return (
    <div className="schedule-place-search" ref={rootRef}>
      <div className="schedule-place-input">
        <input
          value={open ? keyword : selected?.name ?? ''}
          placeholder={places.length === 0 ? '보드에 등록된 장소가 없습니다' : '장소 이름·지역·주소 검색'}
          disabled={disabled || places.length === 0}
          role="combobox"
          aria-expanded={open}
          aria-autocomplete="list"
          onFocus={() => { setOpen(true); setActiveIndex(0) }}
          onChange={(event) => { setKeyword(event.target.value); setOpen(true); setActiveIndex(0) }}
          onKeyDown={handleKeyDown}
        />
        {selected && !open && !disabled && (
          <button type="button" className="schedule-place-clear" aria-label="장소 선택 해제" onClick={() => choose(null)}>×</button>
        )}
      </div>
      {selected && !open && <small className="schedule-place-region">{placeRegion(selected)}</small>}
      {open && (
        <div className="schedule-place-dropdown">
          <div className="schedule-place-filters">
            <select aria-label="장소 지역 필터" value={regionFilter} onChange={(event) => { setRegionFilter(event.target.value); setActiveIndex(0) }}>
              <option value="">모든 지역</option>
              {regionOptions.map((region) => <option key={region} value={region}>{region}</option>)}
            </select>
            <select aria-label="장소 태그 필터" value={tagFilter} onChange={(event) => { setTagFilter(event.target.value); setActiveIndex(0) }}>
              <option value="">모든 태그</option>
              {tags.map((tag) => <option key={tag.tagId} value={tag.name}>{tagLabel(tag.name, tag.icon)}</option>)}
            </select>
            <select aria-label="장소 재방문 필터" value={revisitFilter} onChange={(event) => { setRevisitFilter(event.target.value as RevisitFilter | ''); setActiveIndex(0) }}>
              <option value="">재방문 전체</option>
              <option value="YES">○ 있음</option>
              <option value="MAYBE">△ 고민</option>
              <option value="NO">× 안 감</option>
              <option value="UNREVIEWED">방문 기록 없음</option>
            </select>
          </div>
          <ul className="schedule-place-results" role="listbox">
            {results.length === 0 && <li className="schedule-place-empty">조건에 맞는 장소가 없습니다.</li>}
            {results.map((place, index) => (
              <li
                key={place.boardPlaceId}
                role="option"
                aria-selected={place.boardPlaceId === value}
                className={index === activeIndex ? 'active' : undefined}
                onMouseEnter={() => setActiveIndex(index)}
                onMouseDown={(event) => { event.preventDefault(); choose(place) }}
              >
                <strong>{place.name}</strong>
                <span>{placeRegion(place)}{place.tagNames ? ` · ${place.tagNames}` : ''}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

export function ScheduleModal({
  boardId, places, tags, initialStart, initialAllDay = false, schedule, currentUserId, boardOwnerId, onClose, onSaved,
}: ScheduleModalProps) {
  const defaultEnd = new Date(initialStart.getTime() + 60 * 60 * 1000)
  const [boardPlaceId, setBoardPlaceId] = useState<number | null>(schedule?.boardPlaceId ?? null)
  const [title, setTitle] = useState(schedule?.title ?? '')
  const [allDay, setAllDay] = useState(schedule?.allDay ?? initialAllDay)
  const [startAt, setStartAt] = useState(toInputValue(schedule?.startAt ?? initialStart))
  // 종일 일정의 종료는 서버에 다음 날 0시로 저장되므로, 화면에서는 마지막 날(포함)로 바꿔 보여 준다.
  const [endAt, setEndAt] = useState(schedule?.allDay
    ? `${allDayLastDate(schedule.endAt)}T${toInputValue(defaultEnd).slice(11)}`
    : toInputValue(schedule?.endAt ?? defaultEnd))
  const [memo, setMemo] = useState(schedule?.memo ?? '')
  const [tagIds, setTagIds] = useState<number[]>(schedule?.tagIds?.split(',').map(Number) ?? [])
  const [color, setColor] = useState(schedule?.color ?? SCHEDULE_COLORS[0])
  const [colorPickerOpen, setColorPickerOpen] = useState(false)
  const colorPickerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!colorPickerOpen) return
    const close = (event: MouseEvent) => {
      if (!colorPickerRef.current?.contains(event.target as Node)) setColorPickerOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [colorPickerOpen])

  const saveMutation = useMutation({
    mutationFn: (payload: SchedulePayload) => schedule
      ? updateSchedule(boardId, schedule.scheduleId, payload)
      : createSchedule(boardId, payload),
    onSuccess: onSaved,
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteSchedule(boardId, schedule!.scheduleId),
    onSuccess: onSaved,
  })
  const canEdit = !schedule || schedule.createdBy === currentUserId || boardOwnerId === currentUserId
  const error = saveMutation.error || deleteMutation.error
  const submit = (event: FormEvent) => {
    event.preventDefault()
    const range = allDay ? allDayRange(startAt.slice(0, 10), endAt.slice(0, 10)) : { startAt, endAt }
    saveMutation.mutate({ boardPlaceId, title, ...range, allDay, memo, tagIds, color })
  }

  const selectTag = (tagId: number | null) => {
    setTagIds(tagId === null ? [] : [tagId])
    if (tagId === null) return
    // 정해진 색이 있는 태그(예: 데이트)를 고르면 일정 색도 함께 바꾼다. 이후 색은 직접 다시 고를 수 있다.
    const tagColor = TAG_COLORS[tags.find((tag) => tag.tagId === tagId)?.name.trim() ?? '']
    if (tagColor) setColor(tagColor)
  }

  return (
    <Modal
      title={schedule ? '일정 상세' : '일정 추가'}
      onClose={onClose}
      panelClassName="schedule-modal"
      panelStyle={{ '--schedule-color': color } as CSSProperties}
    >
      <form className="schedule-form" onSubmit={submit}>
        <div className="schedule-form-row schedule-title-row">
          <label htmlFor="schedule-title">일정</label>
          <div className="schedule-title-control" ref={colorPickerRef}>
            <button
              type="button"
              className="schedule-color-trigger"
              style={{ backgroundColor: color }}
              aria-label={`일정 색상: ${SCHEDULE_COLOR_LABELS[color]}. 색상 변경`}
              aria-expanded={colorPickerOpen}
              title="일정 색상 변경"
              disabled={!canEdit}
              onClick={() => setColorPickerOpen((current) => !current)}
            />
            <input id="schedule-title" maxLength={200} value={title} onChange={(event) => setTitle(event.target.value)} disabled={!canEdit} placeholder="무엇을 함께 할까요?" />
            {colorPickerOpen && (
              <div className="schedule-title-color-picker" role="group" aria-label="일정 색상 선택">
                <strong>일정 색상</strong>
                <div className="schedule-color-options">{SCHEDULE_COLORS.map((option) => (
                  <button type="button" key={option} className={color === option ? 'selected' : ''} style={{ backgroundColor: option }} aria-label={SCHEDULE_COLOR_LABELS[option]} aria-pressed={color === option} onClick={() => { setColor(option); setColorPickerOpen(false) }} />
                ))}</div>
              </div>
            )}
            <select
              className="schedule-inline-tag"
              aria-label="일정 태그"
              value={tagIds[0] ?? ''}
              disabled={!canEdit || tags.length === 0}
              onChange={(event) => selectTag(event.target.value ? Number(event.target.value) : null)}
            >
              <option value="">태그 없음</option>
              {tags.map((tag) => <option key={tag.tagId} value={tag.tagId}>{tagLabel(tag.name, tag.icon)}</option>)}
            </select>
          </div>
        </div>
        <div className="schedule-form-row schedule-time-row">
          <span>{allDay ? '날짜' : '시간'}</span>
          <div className="schedule-time-fields">
            <label className="schedule-all-day-toggle">
              <input type="checkbox" checked={allDay} onChange={(event) => setAllDay(event.target.checked)} disabled={!canEdit} />
              종일 (시간 없이 날짜만)
            </label>
            <ScheduleDateTimeField label="시작" value={startAt} onChange={(value) => {
              setStartAt(value)
              // 종일 일정은 시작일을 뒤로 옮기면 종료일도 따라가게 해서 하루짜리 입력을 한 번에 끝낸다.
              if (allDay && value.slice(0, 10) > endAt.slice(0, 10)) setEndAt(`${value.slice(0, 10)}${endAt.slice(10)}`)
            }} disabled={!canEdit} dateOnly={allDay} />
            <ScheduleDateTimeField label={allDay ? '마지막' : '종료'} value={endAt} onChange={setEndAt} disabled={!canEdit} minDate={startAt.slice(0, 10)} dateOnly={allDay} />
          </div>
        </div>
        <div className="schedule-form-row schedule-place-row">
          <span>장소</span>
          <SchedulePlaceSearch places={places} tags={tags} value={boardPlaceId} onChange={setBoardPlaceId} disabled={!canEdit} />
        </div>
        <label className="schedule-form-row schedule-memo-row"><span>메모</span><textarea rows={4} maxLength={2000} value={memo} onChange={(event) => setMemo(event.target.value)} disabled={!canEdit} placeholder="기억해 둘 내용을 적어주세요" /></label>
        {schedule && <p className="schedule-author"><span>작성자</span>{schedule.createdByNickname}</p>}
        {!canEdit && <p className="notice">작성자 또는 보드 소유자만 이 일정을 수정할 수 있습니다.</p>}
        {error && <p className="error-banner">{getApiErrorMessage(error)}</p>}
        <div className="button-row modal-actions schedule-modal-actions">
          {schedule && canEdit && (
            <button type="button" className="danger" disabled={deleteMutation.isPending} onClick={() => {
              if (window.confirm('이 일정을 삭제할까요?')) deleteMutation.mutate()
            }}>삭제</button>
          )}
          <span />
          <button type="button" className="secondary" onClick={onClose}>닫기</button>
          {canEdit && <button disabled={saveMutation.isPending}>{saveMutation.isPending ? '저장 중' : schedule ? '변경사항 저장' : '일정 추가'}</button>}
        </div>
      </form>
    </Modal>
  )
}
