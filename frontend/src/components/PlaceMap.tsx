import { GoogleMap } from './GoogleMap'
import { MapPlace, NaverMap } from './NaverMap'
import { BoardMapType } from '../api/boardApi'
import type { MapSearchTarget } from './mapLocationSearch'

const KOREA_BOUNDS = {
  south: 33,
  north: 39.5,
  west: 124,
  east: 132,
}

function isDomestic(place: MapPlace): boolean {
  const country = place.country?.trim().toUpperCase()
  if (country) {
    return ['KR', 'KOR', '대한민국', '한국', 'SOUTH KOREA', 'REPUBLIC OF KOREA'].includes(country)
  }
  return place.latitude >= KOREA_BOUNDS.south
    && place.latitude <= KOREA_BOUNDS.north
    && place.longitude >= KOREA_BOUNDS.west
    && place.longitude <= KOREA_BOUNDS.east
}

interface PlaceMapProps {
  places: MapPlace[]
  selectedId: number | null
  onSelect: (id: number | null) => void
  mapType?: BoardMapType
  searchTarget?: MapSearchTarget | null
}

export function PlaceMap({ places, selectedId, onSelect, mapType, searchTarget }: PlaceMapProps) {
  const domesticPlaces = places.filter(isDomestic)
  const overseasPlaces = places.filter((place) => !isDomestic(place))
  const selectedPlace = places.find((place) => place.id === selectedId)
  const showNaver = mapType
    ? mapType === 'DOMESTIC'
    : selectedPlace
    ? isDomestic(selectedPlace)
    : domesticPlaces.length > 0 || overseasPlaces.length === 0

  if (showNaver) {
    return <NaverMap places={mapType ? places : domesticPlaces} selectedId={selectedId} onSelect={onSelect} searchTarget={searchTarget} />
  }
  return <GoogleMap places={mapType ? places : overseasPlaces} selectedId={selectedId} onSelect={onSelect} searchTarget={searchTarget} />
}
