import { describe, expect, it } from 'vitest'
import { BoardCyclePeriod, CyclePeriod } from '../api/cycleApi'
import { cycleDayFor, cycleDaysForBoard, statsFor } from './cycleCalendar'

function period(periodId: number, startDate: string, endDate: string): CyclePeriod {
  return { periodId, startDate, endDate, createdAt: `${startDate}T00:00:00`, updatedAt: `${startDate}T00:00:00` }
}

const periods = [period(1, '2026-08-08', '2026-08-14'), period(2, '2026-09-07', '2026-09-13')]

describe('statsFor', () => {
  it('기록의 시작일 간격과 실제 기간으로 평균을 계산한다', () => {
    const result = statsFor(periods)

    expect(result?.averageCycleLength).toBe(30)
    expect(result?.cycleIntervalCount).toBe(1)
    expect(result?.averagePeriodLength).toBe(7)
    expect(result?.nextPeriodStart).toEqual(new Date(2026, 9, 7))
    expect(result?.fertileBeforeStart).toEqual(new Date(2026, 8, 14))
    expect(result?.fertileBeforeEnd).toEqual(new Date(2026, 8, 18))
    expect(result?.ovulationStart).toEqual(new Date(2026, 8, 19))
    expect(result?.ovulationEnd).toEqual(new Date(2026, 8, 21))
    expect(result?.fertileAfterStart).toEqual(new Date(2026, 8, 22))
    expect(result?.fertileAfterEnd).toEqual(new Date(2026, 8, 25))
  })

  it('기록이 한 번이면 기간만 계산하고 주기는 예측하지 않는다', () => {
    const result = statsFor([periods[0]])

    expect(result?.averagePeriodLength).toBe(7)
    expect(result?.averageCycleLength).toBeNull()
    expect(result?.nextPeriodStart).toBeNull()
  })
})

describe('cycleDayFor', () => {
  it('실제 기록과 계산된 일정을 구분해 표시한다', () => {
    expect(cycleDayFor(periods, new Date(2026, 8, 10))?.label).toBe('생리')
    expect(cycleDayFor(periods, new Date(2026, 8, 14))?.label).toBe('가임기')
    expect(cycleDayFor(periods, new Date(2026, 8, 19))?.label).toBe('배란 예정')
    expect(cycleDayFor(periods, new Date(2026, 8, 22))?.label).toBe('가임기')
    expect(cycleDayFor(periods, new Date(2026, 9, 7))?.label).toBe('생리 예정')
    expect(cycleDayFor(periods, new Date(2026, 9, 19))?.label).toBe('배란 예정')
  })
})

describe('cycleDaysForBoard', () => {
  it('보드 구성원별 기록을 섞지 않고 각각 계산한다', () => {
    const sharedPeriods: BoardCyclePeriod[] = [
      ...periods.map((item) => ({ ...item, userId: 1, nickname: '사용자' })),
      { ...period(3, '2026-09-10', '2026-09-14'), userId: 2, nickname: '민수' },
    ]

    const result = cycleDaysForBoard(sharedPeriods, new Date(2026, 8, 10))

    expect(result).toEqual([
      expect.objectContaining({ userId: 1, nickname: '사용자', label: '생리' }),
      expect.objectContaining({ userId: 2, nickname: '민수', label: '생리' }),
    ])
  })
})
