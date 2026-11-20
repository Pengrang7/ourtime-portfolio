import { describe, expect, it } from 'vitest'
import { Schedule } from '../api/scheduleApi'
import { allDayLastDate, allDayRange, eventsOnDay, rangeFor, segmentTimeLabel } from './scheduleCalendar'

function schedule(startAt: string, endAt: string): Schedule {
  return {
    scheduleId: 1,
    boardId: 1,
    boardPlaceId: 1,
    placeName: '테스트 장소',
    placeAddress: '서울',
    latitude: null,
    longitude: null,
    startAt,
    endAt,
    allDay: false,
    memo: null,
    color: '#0B8043',
    tagIds: null,
    tagNames: null,
    tagIcons: null,
    createdBy: 1,
    createdByNickname: '사용자',
    createdAt: startAt,
    updatedAt: startAt,
  }
}

describe('rangeFor', () => {
  it('월 보기에 필요한 6주 범위를 일요일부터 만든다', () => {
    const range = rangeFor('month', new Date(2026, 8, 15))

    expect(range.days).toHaveLength(42)
    expect(range.from.getDay()).toBe(0)
    expect(range.to.getTime()).toBe(range.days[range.days.length - 1].getTime() + 24 * 60 * 60 * 1000)
  })
})

describe('eventsOnDay', () => {
  it('선택일과 일부라도 겹치는 일정만 반환한다', () => {
    const schedules = [
      schedule('2026-09-29T23:00:00', '2026-09-30T01:00:00'),
      schedule('2026-09-30T23:00:00', '2026-10-01T01:00:00'),
      schedule('2026-10-01T00:00:00', '2026-10-01T01:00:00'),
    ]

    expect(eventsOnDay(schedules, new Date(2026, 8, 30))).toHaveLength(2)
  })
})

describe('종일 일정', () => {
  it('마지막 날을 포함한 날짜 범위를 다음 날 0시까지로 저장한다', () => {
    expect(allDayRange('2026-10-30', '2026-11-01')).toEqual({ startAt: '2026-10-30T00:00', endAt: '2026-11-02T00:00' })
    expect(allDayRange('2026-12-31', '2026-12-31').endAt).toBe('2027-01-01T00:00')
    expect(allDayLastDate('2026-11-02T00:00:00')).toBe('2026-11-01')
  })

  it('해당 날짜에만 표시하고 시간 대신 종일로 보여 준다', () => {
    const item = { ...schedule('2026-10-07T00:00:00', '2026-10-08T00:00:00'), allDay: true }

    expect(eventsOnDay([item], new Date(2026, 9, 7))).toHaveLength(1)
    expect(eventsOnDay([item], new Date(2026, 9, 8))).toHaveLength(0)
    expect(segmentTimeLabel(item, new Date(2026, 9, 7))).toBe('종일')
  })
})
