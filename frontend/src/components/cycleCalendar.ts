import { BoardCyclePeriod, CyclePeriod } from '../api/cycleApi'
import { addDays, dateKey, startOfDay } from './scheduleCalendar'

export type CycleDayKind = 'period' | 'fertile' | 'ovulation'

export interface CycleDay {
  kind: CycleDayKind
  label: string
  predicted: boolean
}

export interface BoardCycleDay extends CycleDay {
  userId: number
  nickname: string
}

export interface CycleStats {
  averageCycleLength: number | null
  cycleIntervalCount: number
  averagePeriodLength: number
  nextPeriodStart: Date | null
  nextPeriodEnd: Date | null
  ovulationStart: Date | null
  ovulationEnd: Date | null
  fertileBeforeStart: Date | null
  fertileBeforeEnd: Date | null
  fertileAfterStart: Date | null
  fertileAfterEnd: Date | null
}

function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function differenceInDays(left: Date, right: Date): number {
  const leftUtc = Date.UTC(left.getFullYear(), left.getMonth(), left.getDate())
  const rightUtc = Date.UTC(right.getFullYear(), right.getMonth(), right.getDate())
  return Math.round((leftUtc - rightUtc) / 86_400_000)
}

function average(values: number[]): number {
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

export function statsFor(periods: CyclePeriod[]): CycleStats | null {
  if (periods.length === 0) return null
  const sorted = [...periods].sort((left, right) => left.startDate.localeCompare(right.startDate))
  const periodLengths = sorted.map((period) => differenceInDays(parseLocalDate(period.endDate), parseLocalDate(period.startDate)) + 1)
  // 생리 주기는 종료일이 아니라 연속된 생리 시작일 사이의 일수로 계산한다.
  const startDateIntervals = sorted.slice(1).map((period, index) =>
    differenceInDays(parseLocalDate(period.startDate), parseLocalDate(sorted[index].startDate)))
    .filter((length) => length >= 15 && length <= 90)
  const averagePeriodLength = average(periodLengths)
  const averageCycleLength = startDateIntervals.length > 0 ? average(startDateIntervals) : null
  if (averageCycleLength === null) {
    return {
      averageCycleLength,
      cycleIntervalCount: 0,
      averagePeriodLength,
      nextPeriodStart: null,
      nextPeriodEnd: null,
      ovulationStart: null,
      ovulationEnd: null,
      fertileBeforeStart: null,
      fertileBeforeEnd: null,
      fertileAfterStart: null,
      fertileAfterEnd: null,
    }
  }

  const latestStart = parseLocalDate(sorted[sorted.length - 1].startDate)
  const nextPeriodStart = addDays(latestStart, Math.round(averageCycleLength))
  return {
    averageCycleLength,
    cycleIntervalCount: startDateIntervals.length,
    averagePeriodLength,
    nextPeriodStart,
    nextPeriodEnd: addDays(nextPeriodStart, Math.max(0, Math.round(averagePeriodLength) - 1)),
    fertileBeforeStart: addDays(latestStart, 7),
    fertileBeforeEnd: addDays(latestStart, 11),
    ovulationStart: addDays(latestStart, 12),
    ovulationEnd: addDays(latestStart, 14),
    fertileAfterStart: addDays(latestStart, 15),
    fertileAfterEnd: addDays(latestStart, 18),
  }
}

export function cycleDayFor(periods: CyclePeriod[], day: Date): CycleDay | null {
  const target = dateKey(day)
  const recorded = periods.find((period) => target >= period.startDate && target <= period.endDate)
  if (recorded) return { kind: 'period', label: '생리', predicted: false }

  const stats = statsFor(periods)
  if (!stats?.nextPeriodStart || !stats.nextPeriodEnd) return null
  const latestStartKey = periods.reduce((latest, period) => period.startDate > latest ? period.startDate : latest, periods[0].startDate)
  if (target < latestStartKey) return null

  const cycleLength = Math.round(stats.averageCycleLength!)
  const latestStart = parseLocalDate(latestStartKey)
  const diff = differenceInDays(startOfDay(day), latestStart)
  const cycleNumber = Math.floor(diff / cycleLength)
  const cycleDay = diff % cycleLength
  const periodLength = Math.round(stats.averagePeriodLength)
  if (cycleNumber > 0 && cycleDay < periodLength) return { kind: 'period', label: '생리 예정', predicted: true }
  if (cycleDay >= 12 && cycleDay <= 14) return { kind: 'ovulation', label: '배란 예정', predicted: true }
  if ((cycleDay >= 7 && cycleDay <= 11) || (cycleDay >= 15 && cycleDay <= 18)) {
    return { kind: 'fertile', label: '가임기', predicted: true }
  }
  return null
}

export function cycleDaysForBoard(periods: BoardCyclePeriod[], day: Date): BoardCycleDay[] {
  const periodsByUser = new Map<number, BoardCyclePeriod[]>()
  periods.forEach((period) => {
    const userPeriods = periodsByUser.get(period.userId) ?? []
    userPeriods.push(period)
    periodsByUser.set(period.userId, userPeriods)
  })

  return [...periodsByUser.entries()].flatMap(([userId, userPeriods]) => {
    const cycleDay = cycleDayFor(userPeriods, day)
    return cycleDay ? [{ ...cycleDay, userId, nickname: userPeriods[0].nickname }] : []
  })
}

export function formatDateRange(from: Date, to: Date): string {
  if (dateKey(from) === dateKey(to)) return `${from.getMonth() + 1}월 ${from.getDate()}일`
  return `${from.getMonth() + 1}월 ${from.getDate()}일 ~ ${to.getMonth() + 1}월 ${to.getDate()}일`
}
