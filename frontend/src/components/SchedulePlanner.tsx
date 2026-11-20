import { CSSProperties, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BoardMapType, Tag } from '../api/boardApi'
import { getApiErrorMessage } from '../api/client'
import { BoardCyclePeriod, CyclePeriod, fetchBoardCyclePeriods, fetchCyclePeriods } from '../api/cycleApi'
import { disconnectGoogleCalendar, fetchGoogleCalendarStatus } from '../api/googleCalendarApi'
import { fetchPlaces } from '../api/placeApi'
import { Schedule, fetchSchedules } from '../api/scheduleApi'
import { CyclePeriodModal } from './CyclePeriodModal'
import { ScheduleModal } from './ScheduleModal'
import { ScheduleImportModal } from './ScheduleImportModal'
import { HolidayMap, dayTone, holidayName, mergeHolidays } from './koreanHolidays'
import { fetchHolidays } from '../api/holidayApi'
import { PlaceMap } from './PlaceMap'
import type { MapPlace } from './NaverMap'
import { BoardCycleDay, cycleDaysForBoard } from './cycleCalendar'
import { directionsUrl } from './directions'
import {
  CalendarView,
  WEEKDAY_SHORT,
  addDays,
  dateKey,
  eventsOnDay,
  isToday,
  localDateTime,
  pad,
  rangeFor,
  scheduleTitle,
  segmentTimeLabel,
  startOfDay,
  timeLabel,
} from './scheduleCalendar'

function scheduleStyle(schedule: Schedule): CSSProperties {
  return {
    '--schedule-color': schedule.color,
    '--schedule-background': `${schedule.color}1A`,
  } as CSSProperties
}
interface SchedulePlannerProps {
  boardId: number
  currentUserId: number
  boardOwnerId: number
  tags: Tag[]
  mapType: BoardMapType
}

export function SchedulePlanner({ boardId, currentUserId, boardOwnerId, tags, mapType }: SchedulePlannerProps) {
  const queryClient = useQueryClient()
  const [view, setView] = useState<CalendarView>('week')
  const [cursor, setCursor] = useState(startOfDay(new Date()))
  const [selectedDay, setSelectedDay] = useState(startOfDay(new Date()))
  const [selectedMapScheduleId, setSelectedMapScheduleId] = useState<number | null>(null)
  const [importOpen, setImportOpen] = useState(false)
  const [modal, setModal] = useState<{ schedule: Schedule | null; start: Date; allDay?: boolean } | null>(null)
  const [cycleModalOpen, setCycleModalOpen] = useState(false)
  const range = useMemo(() => rangeFor(view, cursor), [view, cursor])
  const queryKey = ['schedules', boardId, localDateTime(range.from), localDateTime(range.to)]
  const schedulesQuery = useQuery({
    queryKey,
    queryFn: () => fetchSchedules(boardId, localDateTime(range.from), localDateTime(range.to)),
  })
  const placesQuery = useQuery({
    queryKey: ['places', boardId, 'schedule-all'],
    queryFn: () => fetchPlaces(boardId, {}),
  })
  const cycleQuery = useQuery({
    queryKey: ['board-cycle-periods', boardId],
    queryFn: () => fetchBoardCyclePeriods(boardId),
  })
  const myCycleQuery = useQuery({ queryKey: ['cycle-periods'], queryFn: fetchCyclePeriods })
  // 공휴일은 보이는 기간이 걸친 연도 전체를 한 번에 받아 두고, 서버가 실패하면 내장 목록을 쓴다.
  const holidayYears = [range.from.getFullYear(), addDays(range.to, -1).getFullYear()]
  const holidaysQuery = useQuery({
    queryKey: ['holidays', ...holidayYears],
    queryFn: () => fetchHolidays(`${holidayYears[0]}-01-01`, `${holidayYears[1] + 1}-01-01`),
    staleTime: 24 * 60 * 60 * 1000,
    retry: false,
  })
  const holidays = useMemo(() => mergeHolidays(holidaysQuery.data), [holidaysQuery.data])
  const googleCalendarQuery = useQuery({
    queryKey: ['google-calendar-status'],
    queryFn: fetchGoogleCalendarStatus,
  })
  const disconnectGoogleCalendarMutation = useMutation({
    mutationFn: disconnectGoogleCalendar,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['google-calendar-status'] }),
  })
  const schedules = schedulesQuery.data ?? []
  const selectedDaySchedules = eventsOnDay(schedules, selectedDay)
    .sort((left, right) => new Date(left.startAt).getTime() - new Date(right.startAt).getTime())
  const routeMapPlaces = selectedDaySchedules
    .filter((schedule) => schedule.latitude !== null && schedule.longitude !== null)
    .map((schedule, index) => {
      const place = placesQuery.data?.find((item) => item.boardPlaceId === schedule.boardPlaceId)
      return {
        id: schedule.scheduleId,
        name: scheduleTitle(schedule),
        icon: String(index + 1),
        latitude: schedule.latitude!,
        longitude: schedule.longitude!,
        country: place?.country ?? null,
        address: schedule.placeAddress ?? '',
        category: place?.category ?? null,
        phone: place?.phone ?? null,
        tagNames: schedule.tagNames,
        provider: place?.provider ?? '',
        originalUrl: place?.originalUrl ?? '',
        color: schedule.color,
      }
    })

  const title = view === 'day'
    ? `${cursor.getFullYear()}년 ${cursor.getMonth() + 1}월 ${cursor.getDate()}일`
    : view === 'week'
      ? `${range.from.getFullYear()}년 ${range.from.getMonth() + 1}월 ${range.from.getDate()}일 - ${addDays(range.to, -1).getMonth() + 1}월 ${addDays(range.to, -1).getDate()}일`
      : `${cursor.getFullYear()}년 ${cursor.getMonth() + 1}월`

  const move = (direction: number) => {
    const next = view === 'day'
      ? addDays(cursor, direction)
      : view === 'week'
        ? addDays(cursor, direction * 7)
        : new Date(cursor.getFullYear(), cursor.getMonth() + direction, 1)
    setCursor(next)
    setSelectedDay(startOfDay(next))
    setSelectedMapScheduleId(null)
  }
  // 시간 칸을 눌러 열면 시간 일정, 그 외(추가 버튼·종일 칸)는 날짜만 고르는 종일 일정으로 시작한다.
  const openNew = (day = selectedDay, hour?: number) => {
    setSelectedDay(startOfDay(day))
    const start = new Date(day.getFullYear(), day.getMonth(), day.getDate(), hour ?? 9)
    setModal({ schedule: null, start, allDay: hour === undefined })
  }
  const saved = () => {
    setModal(null)
    queryClient.invalidateQueries({ queryKey: ['schedules', boardId] })
  }

  return (
    <section className="planner-shell">
      <header className="planner-toolbar">
        <div className="planner-navigation">
          <button type="button" className="secondary small" onClick={() => move(-1)} aria-label="이전 기간">‹</button>
          <button type="button" className="secondary small" onClick={() => {
            const today = startOfDay(new Date())
            setCursor(today)
            setSelectedDay(today)
          }}>오늘</button>
          <button type="button" className="secondary small" onClick={() => move(1)} aria-label="다음 기간">›</button>
          <h2>{title}</h2>
        </div>
        <div className="planner-actions">
          {googleCalendarQuery.data?.configured && (googleCalendarQuery.data.connected ? (
            <button type="button" className="secondary google-calendar-connected" onClick={() => {
              if (window.confirm('Google Calendar 자동 연동을 해제할까요? 이미 등록된 일정은 Google Calendar에 남습니다.')) {
                disconnectGoogleCalendarMutation.mutate()
              }
            }}>✓ Google 연동됨</button>
          ) : (
            <button type="button" className="secondary" onClick={() => {
              window.location.assign('/api/google-calendar/connect')
            }}>Google Calendar 연동</button>
          ))}
          <div className="view-tabs" aria-label="일정 보기 단위">
            {([['day', '일'], ['week', '주'], ['month', '월']] as const).map(([value, label]) => (
              <button key={value} type="button" className={view === value ? 'active' : ''} onClick={() => setView(value)}>{label}</button>
            ))}
          </div>
          <button type="button" className="secondary" onClick={() => setCycleModalOpen(true)}>🌸 생리 기록</button>
          <button type="button" onClick={() => openNew()}>＋ 일정 추가</button>
          <button type="button" className="secondary" onClick={() => setImportOpen(true)}>📋 한꺼번에 추가</button>
        </div>
      </header>

      {schedulesQuery.error && <p className="error-banner">{getApiErrorMessage(schedulesQuery.error)}</p>}
      {placesQuery.error && <p className="error-banner">{getApiErrorMessage(placesQuery.error)}</p>}
      {cycleQuery.error && <p className="error-banner">{getApiErrorMessage(cycleQuery.error)}</p>}
      {myCycleQuery.error && <p className="error-banner">{getApiErrorMessage(myCycleQuery.error)}</p>}
      {googleCalendarQuery.error && <p className="error-banner">{getApiErrorMessage(googleCalendarQuery.error)}</p>}
      {disconnectGoogleCalendarMutation.error && <p className="error-banner">{getApiErrorMessage(disconnectGoogleCalendarMutation.error)}</p>}

      <div className="schedule-board-view">
        <div className="schedule-calendar-pane">
          {view === 'month' ? (
            <MonthPlanner
              cursor={cursor}
              days={range.days}
              schedules={schedules}
              cyclePeriods={cycleQuery.data ?? []}
              holidays={holidays}
              selectedDay={selectedDay}
              onSelectDay={(day) => {
                setSelectedDay(startOfDay(day))
                setSelectedMapScheduleId(null)
              }}
              onOpen={(schedule) => {
                setSelectedDay(startOfDay(new Date(schedule.startAt)))
                setModal({ schedule, start: new Date(schedule.startAt) })
              }}
            />
          ) : (
            <TimePlanner
              days={range.days}
              schedules={schedules}
              cyclePeriods={cycleQuery.data ?? []}
              holidays={holidays}
              compact={view === 'week'}
              selectedDay={selectedDay}
              onSelectDay={(day) => {
                setSelectedDay(startOfDay(day))
                setSelectedMapScheduleId(null)
              }}
              onNew={openNew}
              onOpen={(schedule) => {
                setSelectedDay(startOfDay(new Date(schedule.startAt)))
                setModal({ schedule, start: new Date(schedule.startAt) })
              }}
            />
          )}
        </div>
        <ScheduleMapPane
          day={selectedDay}
          schedules={selectedDaySchedules}
          places={routeMapPlaces}
          selectedId={selectedMapScheduleId}
          onSelect={setSelectedMapScheduleId}
          mapType={mapType}
        />
      </div>

      {modal && (
        <ScheduleModal
          boardId={boardId}
          places={placesQuery.data ?? []}
          tags={tags}
          initialStart={modal.start}
          initialAllDay={modal.allDay}
          schedule={modal.schedule}
          currentUserId={currentUserId}
          boardOwnerId={boardOwnerId}
          onClose={() => setModal(null)}
          onSaved={saved}
        />
      )}
      {importOpen && (
        <ScheduleImportModal
          boardId={boardId}
          places={placesQuery.data ?? []}
          tags={tags}
          onClose={() => setImportOpen(false)}
          onSaved={() => queryClient.invalidateQueries({ queryKey: ['schedules', boardId] })}
        />
      )}
      {cycleModalOpen && (
        <CyclePeriodModal
          periods={myCycleQuery.data ?? []}
          onClose={() => setCycleModalOpen(false)}
          onChanged={(changedPeriod) => {
            if (changedPeriod) {
              queryClient.setQueryData<CyclePeriod[]>(['cycle-periods'], (current = []) => {
                const exists = current.some((period) => period.periodId === changedPeriod.periodId)
                const next = exists
                  ? current.map((period) => period.periodId === changedPeriod.periodId ? changedPeriod : period)
                  : [changedPeriod, ...current]
                return next.sort((left, right) => right.startDate.localeCompare(left.startDate))
              })
            }
            queryClient.invalidateQueries({ queryKey: ['cycle-periods'] })
            queryClient.invalidateQueries({ queryKey: ['board-cycle-periods', boardId] })
          }}
        />
      )}
    </section>
  )
}

function TimePlanner({ days, schedules, cyclePeriods, holidays, compact, selectedDay, onSelectDay, onNew, onOpen }: {
  days: Date[]
  schedules: Schedule[]
  cyclePeriods: BoardCyclePeriod[]
  holidays: HolidayMap
  compact: boolean
  selectedDay: Date
  onSelectDay: (day: Date) => void
  onNew: (day: Date, hour?: number) => void
  onOpen: (schedule: Schedule) => void
}) {
  return (
    <div className={`time-planner ${compact ? 'weekly' : 'daily'}`}>
      <div className="time-corner">시간</div>
      {days.map((day) => {
        const cycleDays = cycleDaysForBoard(cyclePeriods, day)
        return (
          <button type="button" key={dateKey(day)} className={`time-day-heading ${dayTone(day, holidays) ?? ''} ${isToday(day) ? 'today' : ''} ${dateKey(day) === dateKey(selectedDay) ? 'selected' : ''}`} onClick={() => onSelectDay(day)}>
            <span>{WEEKDAY_SHORT[day.getDay()]}요일</span><strong>{day.getMonth() + 1}/{day.getDate()}</strong>
            {holidayName(day, holidays) && <small className="holiday-name">{holidayName(day, holidays)}</small>}
            <BoardCycleBadge cycleDays={cycleDays} />
          </button>
        )
      })}
      <div className="time-all-day-label">종일</div>
      {days.map((day) => (
        <div key={dateKey(day)} className="time-all-day-cell">
          {eventsOnDay(schedules, day).filter((schedule) => schedule.allDay).map((schedule) => (
            <button
              type="button"
              key={schedule.scheduleId}
              className="time-all-day-event"
              style={scheduleStyle(schedule)}
              onClick={() => onOpen(schedule)}
              title={scheduleTitle(schedule)}
            >
              {scheduleTitle(schedule)}{schedule.memo ? ` · ${schedule.memo}` : ''}
            </button>
          ))}
          <button type="button" className="time-all-day-add" aria-label={`${dateKey(day)} 종일 일정 추가`} onClick={() => onNew(day)} />
        </div>
      ))}
      <div className="hour-labels">
        {Array.from({ length: 24 }, (_, hour) => <div key={hour}>{pad(hour)}:00</div>)}
      </div>
      {days.map((day) => (
        <div key={dateKey(day)} className="time-day-column">
          {Array.from({ length: 24 }, (_, hour) => (
            <button key={hour} type="button" className="time-slot" aria-label={`${dateKey(day)} ${hour}시 일정 추가`} onClick={() => onNew(day, hour)} />
          ))}
          {eventsOnDay(schedules, day).filter((schedule) => !schedule.allDay).map((schedule) => {
            const dayStart = startOfDay(day).getTime()
            const dayEnd = addDays(startOfDay(day), 1).getTime()
            const dayDuration = dayEnd - dayStart
            const start = Math.max(new Date(schedule.startAt).getTime(), dayStart)
            const end = Math.min(new Date(schedule.endAt).getTime(), dayEnd)
            const top = ((start - dayStart) / dayDuration) * 100
            const height = Math.min(100 - top, Math.max(((end - start) / dayDuration) * 100, compact ? 2.2 : 1.8))
            return (
              <button
                type="button"
                key={`${schedule.scheduleId}-${dateKey(day)}`}
                className="schedule-block"
                style={{ ...scheduleStyle(schedule), top: `${top}%`, height: `${height}%` }}
                onClick={() => onOpen(schedule)}
                title={`${scheduleTitle(schedule)}\n${timeLabel(schedule.startAt)} - ${timeLabel(schedule.endAt)}`}
              >
                <strong>{scheduleTitle(schedule)}</strong>
                <span>{segmentTimeLabel(schedule, day)}</span>
                {schedule.memo && <small>{schedule.memo}</small>}
              </button>
            )
          })}
        </div>
      ))}
    </div>
  )
}

function MonthPlanner({ cursor, days, schedules, cyclePeriods, holidays, selectedDay, onSelectDay, onOpen }: {
  cursor: Date
  days: Date[]
  schedules: Schedule[]
  cyclePeriods: BoardCyclePeriod[]
  holidays: HolidayMap
  selectedDay: Date
  onSelectDay: (day: Date) => void
  onOpen: (schedule: Schedule) => void
}) {
  return (
    <div className="month-planner">
      {WEEKDAY_SHORT.map((day, index) => (
        <div key={day} className={`month-weekday ${index === 0 ? 'holiday' : index === 6 ? 'saturday' : ''}`}>{day}</div>
      ))}
      {days.map((day) => {
        const cycleDays = cycleDaysForBoard(cyclePeriods, day)
        return (
          <div key={dateKey(day)} className={`month-day ${dayTone(day, holidays) ?? ''} ${day.getMonth() !== cursor.getMonth() ? 'outside' : ''} ${isToday(day) ? 'today' : ''} ${dateKey(day) === dateKey(selectedDay) ? 'selected' : ''}`}>
            <div className="month-day-header">
              <button type="button" className="month-day-number" onClick={() => onSelectDay(day)} aria-label={`${dateKey(day)} 지도에서 보기`}>{day.getDate()}</button>
              {holidayName(day, holidays) && <span className="holiday-name">{holidayName(day, holidays)}</span>}
              <BoardCycleBadge cycleDays={cycleDays} />
            </div>
            <div className="month-events">
              {eventsOnDay(schedules, day).map((schedule) => (
                <button key={schedule.scheduleId} type="button" className="month-event" style={scheduleStyle(schedule)} onClick={() => onOpen(schedule)}>
                  <span>{segmentTimeLabel(schedule, day)}</span> {scheduleTitle(schedule)}{schedule.memo ? ` · ${schedule.memo}` : ''}
                </button>
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}

function BoardCycleBadge({ cycleDays }: { cycleDays: BoardCycleDay[] }) {
  if (cycleDays.length === 0) return null
  const first = cycleDays[0]
  const details = cycleDays.map((cycleDay) => `${cycleDay.nickname} · ${cycleDay.label}`).join('\n')
  return (
    <small className={`cycle-badge ${first.kind}`} title={details}>
      {first.nickname} · {first.label}{cycleDays.length > 1 ? ` 외 ${cycleDays.length - 1}명` : ''}
    </small>
  )
}

function ScheduleMapPane({ day, schedules, places, selectedId, onSelect, mapType }: {
  day: Date
  schedules: Schedule[]
  places: MapPlace[]
  selectedId: number | null
  onSelect: (id: number | null) => void
  mapType: BoardMapType
}) {
  return (
    <section className="schedule-map-pane">
      <div className="route-map">
        {places.length > 0
          ? <PlaceMap places={places} selectedId={selectedId} onSelect={onSelect} mapType={mapType} />
          : <p className="map-message">선택한 날짜에 좌표가 있는 일정 장소가 없습니다.</p>}
        {schedules.length > 0 && <ol className="route-list map-route-legend">
          {schedules.map((schedule) => {
            const markerIcon = places.find((place) => place.id === schedule.scheduleId)?.icon ?? '–'
            const title = schedule.title || schedule.placeName || '개인 일정'
            const tagIcon = schedule.tagIcons?.trim().split(/\s+/)[0] || '–'
            return <li key={schedule.scheduleId} className={selectedId === schedule.scheduleId ? 'selected' : ''} style={scheduleStyle(schedule)}>
              <button
                type="button"
                className="route-summary"
                onClick={() => onSelect(schedule.scheduleId)}
                aria-label={`${title} 일정 보기`}
              >
                <b>{markerIcon}</b>
                <span className="route-tag" title={schedule.tagNames ?? undefined}>{tagIcon}</span>
                <span className="route-title-line">
                  <strong className="route-place">{title}</strong>
                  <small className="route-time">{segmentTimeLabel(schedule, day)}</small>
                </span>
              </button>
              {(schedule.memo || (schedule.latitude !== null && schedule.longitude !== null)) && (
                <div className="route-subline">
                  {schedule.memo && <span className="route-memo" title={schedule.memo}>{schedule.memo}</span>}
                  {schedule.memo && schedule.latitude !== null && schedule.longitude !== null && <i aria-hidden="true">|</i>}
                  {schedule.latitude !== null && schedule.longitude !== null && (
                    <a
                      className="link-button"
                      href={directionsUrl({
                        latitude: schedule.latitude,
                        longitude: schedule.longitude,
                        name: schedule.placeName || schedule.title || '목적지',
                        mapType,
                      })}
                      target="_blank"
                      rel="noreferrer"
                    >{mapType === 'DOMESTIC' ? '네이버 길찾기' : '길찾기'}</a>
                  )}
                </div>
              )}
            </li>
          })}
        </ol>}
      </div>
    </section>
  )
}
