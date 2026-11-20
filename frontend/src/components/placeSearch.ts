import { BoardPlace, RevisitFilter } from '../api/placeApi'

/** 장소 목록 두 번째 줄에 보여 줄 지역. 행정구역 정보가 없으면 주소로 대신한다. */
export function placeRegion(place: BoardPlace): string {
  const region = [place.sido, place.sigungu, place.dong].filter(Boolean).join(' ')
  return region || place.address || ''
}

function normalize(value: string): string {
  return value.toLowerCase().replace(/\s+/g, '')
}

/** 이름·주소·지역·카테고리·태그 중 어디에든 검색어가 포함되면(%검색어%) 일치로 본다. 공백은 무시한다. */
export function filterPlaces(
  places: BoardPlace[],
  keyword: string,
  filters: { region?: string; tag?: string; revisit?: RevisitFilter } = {},
): BoardPlace[] {
  const target = normalize(keyword)
  return places.filter((place) => {
    const keywordMatches = !target || normalize([
      place.name, place.address, place.sido, place.sigungu, place.dong, place.category, place.tagNames,
    ].filter(Boolean).join(' ')).includes(target)
    const region = [place.sido, place.sigungu].filter(Boolean).join(' ')
    const regionMatches = !filters.region || place.sido === filters.region || region === filters.region
    const tagMatches = !filters.tag || (place.tagNames?.split(',').filter(Boolean) ?? []).includes(filters.tag)
    const reviewCount = place.revisitYesCount + place.revisitMaybeCount + place.revisitNoCount
    const revisitMatches = !filters.revisit
      || (filters.revisit === 'YES' && place.revisitYesCount > 0)
      || (filters.revisit === 'MAYBE' && place.revisitMaybeCount > 0)
      || (filters.revisit === 'NO' && place.revisitNoCount > 0)
      || (filters.revisit === 'UNREVIEWED' && reviewCount === 0)
    return keywordMatches && regionMatches && tagMatches && revisitMatches
  })
}
