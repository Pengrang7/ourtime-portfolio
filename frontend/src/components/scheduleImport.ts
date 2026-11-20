import { BoardPlace } from '../api/placeApi'
import { SCHEDULE_COLORS, SchedulePayload } from '../api/scheduleApi'
import { addDays, allDayRange, dateKey, pad, WEEKDAY_SHORT } from './scheduleCalendar'
import { filterPlaces } from './placeSearch'

/** 붙여넣은 한 줄을 일정 한 건으로 해석한 결과. 저장 전에 화면에서 고칠 수 있다. */
export interface ImportedSchedule {
  line: string
  startDate: string
  lastDate: string
  allDay: boolean
  startTime: string
  endTime: string
  boardPlaceId: number | null
  title: string
  memo: string
  warnings: string[]
}

export interface ImportError {
  line: string
  error: string
}

const WEEKDAY = '\\(?\\s*([월화수목금토일])(?:요일)?\\s*\\)?'
const DATE_PATTERN = new RegExp(
  `^\\s*(?:(\\d{4})\\s*[년./-]\\s*)?(\\d{1,2})\\s*(?:월|[./-])\\s*(\\d{1,2})\\s*일?\\s*(?:${WEEKDAY})?`)
const RANGE_PATTERN = new RegExp(
  `^\\s*[~～]\\s*(?:(?:(\\d{1,2})\\s*(?:월|[./-])\\s*)?(\\d{1,2})\\s*일?\\s*(?:${WEEKDAY})?)?`)
const TIME_PATTERN = /(오전|오후|아침|저녁|밤|낮)?\s*(\d{1,2})(?:\s*시(?:\s*(\d{1,2})\s*분|\s*(반))?|:(\d{2}))/g
const URL_PATTERN = /https?:\/\/\S+/g

function normalize(value: string): string {
  return value.toLowerCase().replace(/\s+/g, '')
}

function toHour(meridiem: string | undefined, hour: number): number {
  if ((meridiem === '오후' || meridiem === '저녁' || meridiem === '밤') && hour < 12) return hour + 12
  if (meridiem === '오전' && hour === 12) return 0
  return hour
}

/** "오전 10시부터 오후 6시" 처럼 시간이 들어 있으면 시작·종료 시각과 시간을 뺀 나머지 글자를 돌려준다. */
function extractTimes(text: string): { times: string[]; rest: string } {
  const times: string[] = []
  const rest = text.replace(TIME_PATTERN, (_, meridiem: string | undefined, hour: string, minute?: string, half?: string, colonMinute?: string) => {
    const minutes = colonMinute ?? minute ?? (half ? '30' : '0')
    times.push(`${pad(toHour(meridiem, Number(hour)))}:${pad(Number(minutes))}`)
    return ' '
  })
  return { times, rest: rest.replace(/부터|까지|[~～-]/g, ' ').replace(/\s+/g, ' ').trim() }
}

/** 연도를 적지 않았으면 올해로 보되, 두 달 넘게 지난 날짜면 내년 일정으로 본다. */
function resolveDate(year: string | undefined, month: number, day: number, today: Date): Date {
  if (year) return new Date(Number(year), month - 1, day)
  const candidate = new Date(today.getFullYear(), month - 1, day)
  return candidate < addDays(today, -60) ? new Date(today.getFullYear() + 1, month - 1, day) : candidate
}

function matchPlace(text: string, places: BoardPlace[]): BoardPlace | null {
  const target = normalize(text)
  if (!target) return null
  const exact = places.find((place) => normalize(place.name) === target)
  if (exact) return exact
  const candidates = filterPlaces(places, text)
  return candidates.length === 1 ? candidates[0] : null
}

export function parseScheduleText(text: string, places: BoardPlace[], today = new Date()): {
  schedules: ImportedSchedule[]
  errors: ImportError[]
} {
  const schedules: ImportedSchedule[] = []
  const errors: ImportError[] = []
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.replace(/^\s*(?:[-*•·]|\d+[.)])\s+/, '').trim()
    if (!line) continue
    const dateMatch = DATE_PATTERN.exec(line)
    if (!dateMatch) {
      errors.push({ line, error: '맨 앞에서 날짜(예: 10월 7일)를 찾지 못했어요.' })
      continue
    }
    const [, year, monthText, dayText, weekdayText] = dateMatch
    const month = Number(monthText)
    const day = Number(dayText)
    const start = resolveDate(year, month, day, today)
    if (start.getMonth() !== month - 1 || start.getDate() !== day) {
      errors.push({ line, error: `${month}월 ${day}일은 없는 날짜예요.` })
      continue
    }

    const warnings: string[] = []
    if (weekdayText && WEEKDAY_SHORT[start.getDay()] !== weekdayText) {
      warnings.push(`${start.getFullYear()}년 ${month}월 ${day}일은 ${WEEKDAY_SHORT[start.getDay()]}요일이에요. 날짜를 확인해 주세요.`)
    }

    let rest = line.slice(dateMatch[0].length)
    let last = start
    const rangeMatch = RANGE_PATTERN.exec(rest)
    if (rangeMatch) {
      rest = rest.slice(rangeMatch[0].length)
      const [, endMonthText, endDayText] = rangeMatch
      if (endDayText) {
        const endMonth = endMonthText ? Number(endMonthText) : month
        last = new Date(start.getFullYear(), endMonth - 1, Number(endDayText))
        // "12월 30일~1월 2일"은 다음 해로, "10월 30일~2일"은 다음 달로 넘어간 것으로 본다.
        if (last < start) {
          last = endMonthText
            ? new Date(start.getFullYear() + 1, endMonth - 1, Number(endDayText))
            : new Date(start.getFullYear(), month, Number(endDayText))
        }
      } else {
        warnings.push('끝나는 날짜가 없어서 하루 일정으로 넣었어요. 마지막 날을 골라 주세요.')
      }
    }

    const urls = rest.match(URL_PATTERN) ?? []
    const parts = rest.replace(URL_PATTERN, ' ').split(/[,，]/).map((part) => part.trim()).filter(Boolean)
    let times: string[] = []
    const textParts: string[] = []
    for (const part of parts) {
      const extracted = extractTimes(part)
      if (extracted.times.length > 0 && times.length === 0) {
        times = extracted.times
        if (extracted.rest) textParts.push(extracted.rest)
      } else {
        textParts.push(part)
      }
    }

    const [placeText = '', ...memoParts] = textParts
    const place = matchPlace(placeText, places)
    let startTime = '09:00'
    let endTime = '10:00'
    if (times.length > 0) {
      startTime = times[0]
      const [hour, minute] = startTime.split(':').map(Number)
      endTime = times[1] ?? `${pad(Math.min(hour + 1, 23))}:${pad(minute)}`
      // "10시부터 6시"처럼 오후를 생략한 종료 시각은 오후로 본다.
      if (endTime <= startTime && Number(endTime.slice(0, 2)) < 12) {
        endTime = `${pad(Number(endTime.slice(0, 2)) + 12)}:${endTime.slice(3)}`
      }
    }

    schedules.push({
      line,
      startDate: dateKey(start),
      lastDate: dateKey(last),
      allDay: times.length === 0,
      startTime,
      endTime,
      boardPlaceId: place?.boardPlaceId ?? null,
      title: place ? '' : placeText,
      memo: [...memoParts, ...urls].join('\n'),
      warnings,
    })
  }
  return { schedules, errors }
}

export function toSchedulePayload(item: ImportedSchedule): SchedulePayload {
  const range = item.allDay
    ? allDayRange(item.startDate, item.lastDate)
    : { startAt: `${item.startDate}T${item.startTime}`, endAt: `${item.lastDate}T${item.endTime}` }
  return {
    boardPlaceId: item.boardPlaceId,
    title: item.title,
    ...range,
    allDay: item.allDay,
    memo: item.memo,
    tagIds: [],
    color: SCHEDULE_COLORS[0],
  }
}
