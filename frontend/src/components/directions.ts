import { BoardMapType } from '../api/boardApi'

export type TravelMode = 'transit' | 'driving' | 'walking'

interface DirectionsOptions {
  latitude: number
  longitude: number
  name: string
  mapType: BoardMapType
  travelMode?: TravelMode
}

export function directionsUrl({
  latitude, longitude, name, mapType, travelMode = 'transit',
}: DirectionsOptions): string {
  if (mapType === 'DOMESTIC') {
    const route = travelMode === 'driving' ? 'car' : travelMode === 'walking' ? 'walk' : 'transit'
    const projected = webMercator(latitude, longitude)
    const destination = [
      projected.x,
      projected.y,
      encodeURIComponent(name || '목적지'),
      '',
      'PLACE_POI',
    ].join(',')
    return `https://map.naver.com/p/directions/-/${destination}/-/${route}`
  }

  const params = new URLSearchParams({
    api: '1',
    destination: `${latitude},${longitude}`,
    travelmode: travelMode,
  })
  return `https://www.google.com/maps/dir/?${params.toString()}`
}

// 네이버 지도 웹 길찾기 URL은 WGS84 위·경도가 아니라 Web Mercator 좌표를 사용한다.
function webMercator(latitude: number, longitude: number): { x: number; y: number } {
  const maxLatitude = 85.05112878
  const boundedLatitude = Math.max(-maxLatitude, Math.min(maxLatitude, latitude))
  const earthRadius = 6378137
  const x = earthRadius * longitude * Math.PI / 180
  const y = earthRadius * Math.log(Math.tan(Math.PI / 4 + boundedLatitude * Math.PI / 360))
  return { x, y }
}
