import { describe, expect, it } from 'vitest'
import { dayTone, holidayName, mergeHolidays } from './koreanHolidays'

describe('koreanHolidays', () => {
  it('공휴일과 대체공휴일 이름을 알려 준다', () => {
    expect(holidayName(new Date(2026, 9, 9))).toBe('한글날')
    expect(holidayName(new Date(2026, 9, 5))).toBe('대체공휴일(개천절)')
    expect(holidayName(new Date(2026, 9, 6))).toBeNull()
  })

  it('일요일·공휴일은 빨간색, 공휴일이 아닌 토요일은 파란색이다', () => {
    expect(dayTone(new Date(2026, 9, 4))).toBe('holiday')
    expect(dayTone(new Date(2026, 9, 5))).toBe('holiday')
    expect(dayTone(new Date(2026, 9, 10))).toBe('saturday')
    expect(dayTone(new Date(2026, 9, 3))).toBe('holiday')
    expect(dayTone(new Date(2026, 9, 7))).toBeNull()
  })

  it('서버에서 받은 공휴일을 내장 목록보다 우선하고 새 연도도 표시한다', () => {
    const holidays = mergeHolidays([
      { date: '2026-01-01', name: '새해첫날' },
      { date: '2028-01-01', name: '새해첫날' },
    ])

    expect(holidayName(new Date(2026, 0, 1), holidays)).toBe('새해첫날')
    expect(dayTone(new Date(2028, 0, 1), holidays)).toBe('holiday')
    expect(holidayName(new Date(2026, 9, 9), holidays)).toBe('한글날')
  })
})
