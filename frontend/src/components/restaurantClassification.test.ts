import { describe, expect, it } from 'vitest'
import {
  classifyFallbackRestaurant,
  mergeFallbackRestaurantTag,
} from './restaurantClassification'

describe('기타·밥 추가 분류', () => {
  it.each([
    ['미당', '베트남음식', '아시아'],
    ['1987 신샤브', '음식점', '아시아'],
    ['홍콩구락부', 'BAR', '술'],
    ['성수옥상', '와인바', '술'],
    ['분류가 애매한 집', '음식점', '밥'],
  ])('%s을(를) %s로 분류한다', (name, category, target) => {
    expect(classifyFallbackRestaurant({ name, category })).toBe(target)
  })

  it('기타와 밥만 교체하고 기존 태그는 보존한다', () => {
    expect(mergeFallbackRestaurantTag(['기타', '밥', '데이트', '한식'], '술'))
      .toEqual(['데이트', '한식', '술'])
  })
})
