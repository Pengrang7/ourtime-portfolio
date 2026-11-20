import { describe, expect, it } from 'vitest'
import { directionsUrl } from './directions'

describe('directionsUrl', () => {
  it('국내 장소는 브라우저에서 열 수 있는 네이버 지도 HTTPS 길찾기 URL을 만든다', () => {
    const url = directionsUrl({
      latitude: 35.8688142,
      longitude: 128.5930295,
      name: '크레이지팡팡',
      mapType: 'DOMESTIC',
    })

    expect(url).toMatch(/^https:\/\/map\.naver\.com\/p\/directions\/-\//)
    expect(url).toContain(encodeURIComponent('크레이지팡팡'))
    expect(url.endsWith('/-/transit')).toBe(true)
    expect(url).not.toContain('nmap://')
  })

  it('해외 장소는 Google Maps 길찾기 URL을 만든다', () => {
    const url = directionsUrl({
      latitude: 35.6812,
      longitude: 139.7671,
      name: '도쿄역',
      mapType: 'OVERSEAS',
      travelMode: 'walking',
    })

    expect(url).toContain('https://www.google.com/maps/dir/')
    expect(url).toContain('destination=35.6812%2C139.7671')
    expect(url).toContain('travelmode=walking')
  })
})
