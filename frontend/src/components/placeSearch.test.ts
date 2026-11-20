import { describe, expect, it } from 'vitest'
import { BoardPlace } from '../api/placeApi'
import { filterPlaces, placeRegion } from './placeSearch'

function place(boardPlaceId: number, name: string, overrides: Partial<BoardPlace> = {}): BoardPlace {
  return {
    boardPlaceId, placeId: boardPlaceId, name, address: '', latitude: null, longitude: null, country: null,
    sido: null, sigungu: null, dong: null, category: null, phone: null, provider: 'NAVER', providerPlaceId: null,
    originalUrl: '', sourceStatus: 'ACTIVE', lastVerifiedAt: null,
    revisitYesCount: 0, revisitMaybeCount: 0, revisitNoCount: 0, createdBy: 1, createdByNickname: '', createdAt: '',
    tagNames: null, ...overrides,
  }
}

const places = [
  place(1, '스타벅스 강남역점', { sido: '서울', sigungu: '강남구', dong: '역삼동', address: '서울 강남구 강남대로 390' }),
  place(2, '런던 베이글 뮤지엄', { sido: '서울', sigungu: '종로구', address: '서울 종로구 북촌로4길 20', tagNames: '빵집' }),
]

describe('filterPlaces', () => {
  it('이름 중간에 들어간 검색어도 찾는다', () => {
    expect(filterPlaces(places, '베이글').map((item) => item.boardPlaceId)).toEqual([2])
  })

  it('지역과 태그로도 찾고 공백과 대소문자는 무시한다', () => {
    expect(filterPlaces(places, '강남 구').map((item) => item.boardPlaceId)).toEqual([1])
    expect(filterPlaces(places, '빵집').map((item) => item.boardPlaceId)).toEqual([2])
  })

  it('검색어가 비어 있으면 전체를 돌려준다', () => {
    expect(filterPlaces(places, '  ')).toHaveLength(2)
  })

  it('지역과 태그 필터를 검색어와 함께 적용한다', () => {
    expect(filterPlaces(places, '', { region: '서울 강남구' }).map((item) => item.boardPlaceId)).toEqual([1])
    expect(filterPlaces(places, '', { tag: '빵집' }).map((item) => item.boardPlaceId)).toEqual([2])
    expect(filterPlaces(places, '베이글', { region: '서울 강남구' })).toEqual([])
  })

  it('멤버 중 한 명이라도 선택한 재방문 의사로 필터링한다', () => {
    const reviewed = [
      place(1, '둘 다 기록', { revisitYesCount: 1, revisitNoCount: 1 }),
      place(2, '기록 없음'),
    ]
    expect(filterPlaces(reviewed, '', { revisit: 'YES' }).map((item) => item.boardPlaceId)).toEqual([1])
    expect(filterPlaces(reviewed, '', { revisit: 'NO' }).map((item) => item.boardPlaceId)).toEqual([1])
    expect(filterPlaces(reviewed, '', { revisit: 'UNREVIEWED' }).map((item) => item.boardPlaceId)).toEqual([2])
  })
})

describe('placeRegion', () => {
  it('행정구역이 있으면 지역을, 없으면 주소를 보여 준다', () => {
    expect(placeRegion(places[0])).toBe('서울 강남구 역삼동')
    expect(placeRegion(place(3, '집', { address: '경기 성남시' }))).toBe('경기 성남시')
  })
})
