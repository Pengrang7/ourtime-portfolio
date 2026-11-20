import { Schedule } from '../api/scheduleApi'

export type CalendarView = 'day' | 'week' | 'month'

export const WEEKDAY_SHORT = ['일', '월', '화', '수', '목', '금', '토']

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

export function addDays(date: Date, days: number): Date {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function startOfWeek(date: Date): Date {
  const day = startOfDay(date)
  return addDays(day, -day.getDay())
}

function startOfMonthGrid(date: Date): Date {
  return startOfWeek(new Date(date.getFullYear(), date.getMonth(), 1))
}

export function pad(value: number): string {
  return String(value).padStart(2, '0')
}

export function localDateTime(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function dateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function timeLabel(value: string): string {
  const date = new Date(value)
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function segmentTimeLabel(schedule: Schedule, day: Date): string {
  if (schedule.allDay) return '종일'
  const start = new Date(schedule.startAt)
  const end = new Date(schedule.endAt)
  const startsToday = dateKey(start) === dateKey(day)
  const endsToday = dateKey(end) === dateKey(day)
  if (startsToday && endsToday) return `${timeLabel(schedule.startAt)}–${timeLabel(schedule.endAt)}`
  if (startsToday) return `${timeLabel(schedule.startAt)} →`
  if (endsToday) return `→ ${timeLabel(schedule.endAt)} 종료`
  return '↔ 계속'
}

function parseDateKey(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

/** 종일 일정의 시작일·마지막 날(포함)을 서버 저장 형식(종료는 다음 날 0시, 배타적)으로 바꾼다. */
export function allDayRange(startDate: string, lastDate: string): { startAt: string; endAt: string } {
  return { startAt: `${startDate}T00:00`, endAt: `${dateKey(addDays(parseDateKey(lastDate), 1))}T00:00` }
}

/** 서버의 배타적 종료 시각을 화면에 보여 줄 마지막 날(포함)로 바꾼다. */
export function allDayLastDate(endAt: string): string {
  return dateKey(addDays(parseDateKey(endAt.slice(0, 10)), -1))
}

export function scheduleTitle(schedule: Schedule): string {
  const title = schedule.title || schedule.placeName || '개인 일정'
  return `${schedule.tagIcons ? `${schedule.tagIcons} ` : ''}${title}`
}

export function isToday(date: Date): boolean {
  return dateKey(date) === dateKey(new Date())
}

export function rangeFor(view: CalendarView, cursor: Date): { from: Date; to: Date; days: Date[] } {
  const from = view === 'day' ? startOfDay(cursor) : view === 'week' ? startOfWeek(cursor) : startOfMonthGrid(cursor)
  const count = view === 'day' ? 1 : view === 'week' ? 7 : 42
  return { from, to: addDays(from, count), days: Array.from({ length: count }, (_, index) => addDays(from, index)) }
}

export function eventsOnDay(schedules: Schedule[], day: Date): Schedule[] {
  const from = startOfDay(day).getTime()
  const to = addDays(startOfDay(day), 1).getTime()
  return schedules.filter((schedule) => new Date(schedule.startAt).getTime() < to && new Date(schedule.endAt).getTime() > from)
}
