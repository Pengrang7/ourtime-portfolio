import { BoardPlace } from '../api/placeApi'

export const FALLBACK_SOURCE_TAGS = ['기타', '밥'] as const
export const FALLBACK_TARGET_TAGS = ['밥', '아시아', '술'] as const

export type FallbackRestaurantTag = typeof FALLBACK_TARGET_TAGS[number]

const ALCOHOL = /\bbar\b|술집|주점|요리주점|와인|펍|\bpub\b|호프|맥주|칵테일|이자카야|사케|포차|막걸리|위스키|하이볼/
const ASIAN = /아시아|동남아|베트남|태국|타이|인도|인도네시아|말레이시아|싱가포르|필리핀|캄보디아|라오스|미얀마|네팔|스리랑카|중동|터키|튀르키예|쌀국수|팟타이|똠얌|분짜|반미|커리|탄두리|샤브|스키야키/

export function classifyFallbackRestaurant(
  place: Pick<BoardPlace, 'name' | 'category'>,
): FallbackRestaurantTag {
  const text = `${place.name} ${place.category ?? ''}`.toLocaleLowerCase('ko-KR')
  if (ALCOHOL.test(text)) return '술'
  if (ASIAN.test(text)) return '아시아'
  return '밥'
}

export function mergeFallbackRestaurantTag(
  currentNames: string[],
  target: FallbackRestaurantTag,
): string[] {
  const sourceNames = new Set<string>(FALLBACK_SOURCE_TAGS)
  return [...new Set([
    ...currentNames.filter((name) => !sourceNames.has(name)),
    target,
  ])]
}
