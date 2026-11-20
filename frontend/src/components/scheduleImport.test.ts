import { describe, expect, it } from 'vitest'
import { BoardPlace } from '../api/placeApi'
import { parseScheduleText, toSchedulePayload } from './scheduleImport'

function place(boardPlaceId: number, name: string): BoardPlace {
  return {
    boardPlaceId, placeId: boardPlaceId, name, address: '', latitude: null, longitude: null, country: null,
    sido: null, sigungu: null, dong: null, category: null, phone: null, provider: 'NAVER', providerPlaceId: null,
    originalUrl: '', sourceStatus: 'ACTIVE', lastVerifiedAt: null,
    revisitYesCount: 0, revisitMaybeCount: 0, revisitNoCount: 0, createdBy: 1, createdByNickname: '', createdAt: '', tagNames: null,
  }
}

const today = new Date(2026, 9, 6)
const places = [place(1, '에버랜드'), place(2, '서초문화예술공원')]

describe('parseScheduleText', () => {
  it('날짜, 장소, 메모만 있는 줄은 종일 일정으로 만든다', () => {
    const { schedules } = parseScheduleText('10월 7일 수요일, 성균관대역, 옷 챙겨서', places, today)

    expect(schedules[0]).toMatchObject({
      startDate: '2026-10-07', lastDate: '2026-10-07', allDay: true,
      boardPlaceId: null, title: '성균관대역', memo: '옷 챙겨서', warnings: [],
    })
  })

  it('보드에 있는 장소는 연결하고 시간과 링크를 따로 뽑는다', () => {
    const { schedules } = parseScheduleText(
      '10월 17일(토), 서초문화예술공원, 오전 10시부터 오후 6시 - https://seochoai.kr/', places, today)

    expect(schedules[0]).toMatchObject({
      startDate: '2026-10-17', allDay: false, startTime: '10:00', endTime: '18:00',
      boardPlaceId: 2, title: '', memo: 'https://seochoai.kr/',
    })
  })

  it('끝 날짜 없는 물결표는 하루로 넣고 확인 안내를 남긴다', () => {
    const { schedules } = parseScheduleText('10월 30일 금요일~, 경주', places, today)

    expect(schedules[0].lastDate).toBe('2026-10-30')
    expect(schedules[0].title).toBe('경주')
    expect(schedules[0].warnings).toHaveLength(1)
  })

  it('기간과 요일 오류, 날짜 없는 줄을 구분한다', () => {
    const { schedules, errors } = parseScheduleText(
      '10월 30일~11월 1일, 경주\n10월 24일 금요일, 에버랜드\n메모만 있는 줄', places, today)

    expect(schedules[0]).toMatchObject({ startDate: '2026-10-30', lastDate: '2026-11-01' })
    expect(schedules[1]).toMatchObject({ boardPlaceId: 1 })
    expect(schedules[1].warnings[0]).toContain('토요일')
    expect(errors).toEqual([{ line: '메모만 있는 줄', error: expect.any(String) }])
  })

  it('두 달 넘게 지난 날짜는 내년으로 본다', () => {
    const { schedules } = parseScheduleText('1월 3일, 신년회', places, today)

    expect(schedules[0].startDate).toBe('2027-01-03')
  })
})

describe('toSchedulePayload', () => {
  it('종일 일정은 마지막 날 다음 날 0시까지로 저장한다', () => {
    const { schedules } = parseScheduleText('10월 30일~11월 1일, 경주', places, today)

    expect(toSchedulePayload(schedules[0])).toMatchObject({
      startAt: '2026-10-30T00:00', endAt: '2026-11-02T00:00', allDay: true, title: '경주',
    })
  })
})
