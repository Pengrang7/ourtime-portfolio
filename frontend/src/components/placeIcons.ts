// 업종 문자열(또는 새 태그 이름)에 포함된 키워드로 아이콘을 추천한다. 위에 있는 규칙이 우선한다.
const ICON_RULES: Array<[string, string[]]> = [
  ['🍮', ['푸딩']],
  ['🎢', ['놀이공원', '테마파크', '놀이동산', '워터파크', '롯데월드', '에버랜드']],
  ['🐾', ['동물원', '아쿠아리움', '수족관', '애견', '반려']],
  ['🍰', ['디저트', '베이커리', '빵', '케이크', '도넛', '아이스크림', '젤라또']],
  ['🧸', ['키즈', '어린이', '놀이터', '아이랑']],
  ['🧃', ['주스', '스무디', '생과일']],
  ['☕', ['카페', '커피', '티룸', '찻집']],
  ['🍺', ['술집', '주점', '호프', '맥주', '펍', '와인', '칵테일', '이자카야', '포차']],
  ['🍣', ['초밥', '스시', '일식', '회']],
  ['🍜', ['아시아', '동남아', '베트남', '태국', '인도', '중동']],
  ['🍜', ['라멘', '국수', '면', '쌀국수', '중식', '짜장']],
  ['🍖', ['고기', '갈비', '삼겹', '구이', '바베큐', '스테이크']],
  ['🍕', ['피자', '파스타', '양식', '이탈리안', '버거', '햄버거']],
  ['🍽️', ['식당', '맛집', '음식', '한식', '레스토랑', '밥', '분식', '뷔페']],
  ['🏛️', ['박물관', '미술관', '전시', '갤러리', '역사', '궁']],
  ['🎭', ['공연', '극장', '영화', '뮤지컬', '콘서트']],
  ['🏖️', ['해변', '바다', '해수욕', '섬']],
  ['⛰️', ['등산', '계곡', '캠핑', '글램핑']],
  ['🌳', ['공원', '수목원', '정원', '산책', '숲']],
  ['🛍️', ['쇼핑', '시장', '백화점', '아울렛', '마트', '소품']],
  ['🏨', ['숙소', '호텔', '펜션', '리조트', '게스트하우스', '모텔', '스테이']],
  ['♨️', ['온천', '스파', '찜질']],
  ['🎮', ['게임', '방탈출', '볼링', '노래방', '오락', 'pc방']],
  ['💆', ['마사지', '네일', '미용', '헤어']],
  ['📸', ['사진', '포토', '뷰', '전망', '야경']],
]

export const DEFAULT_PLACE_ICON = '📍'

// 태그 아이콘 선택지. 사용자가 태그마다 하나를 고른다.
export const TAG_ICON_CHOICES = [
  '🎢', '🧸', '🐾', '☕', '🧃', '🍰', '🍮', '🍚', '🥟', '🍝', '🍤', '🥂', '👨‍🍳', '🍽️', '🍜', '🍣', '🍖', '🍕', '🍺',
  '🏛️', '🎭', '🎮', '📸', '🌳', '⛰️', '🏖️', '♨️', '🛍️', '🏨', '💆', '⭐',
  '❤️', '👍', '🚗', '🚶', '👶', '🐶', '🌸', '🌙', '🎉', '📍',
]

export function iconFor(text: string | null | undefined): string | null {
  if (!text) return null
  const normalized = text.toLowerCase().replace(/\s/g, '')
  return ICON_RULES.find(([, keywords]) => keywords.some((keyword) => normalized.includes(keyword)))?.[0] ?? null
}

export type TagIconMap = Map<string, string | null>

export function tagIconMap(tags: { name: string; icon: string | null }[]): TagIconMap {
  return new Map(tags.map((tag) => [tag.name, tag.icon]))
}

export function tagLabel(name: string, icon: string | null | undefined): string {
  return `${icon || '#'}${name}`
}

// 아이콘을 고른 태그를 먼저 보고, 없으면 지도 서비스의 업종으로 판단한다.
export function placeIcon(tagNames: string[], icons: TagIconMap, category: string | null): string {
  for (const tag of tagNames) {
    const icon = icons.get(tag)
    if (icon) return icon
  }
  return iconFor(category) ?? DEFAULT_PLACE_ICON
}

export function splitTagNames(tagNames: string | null): string[] {
  return tagNames ? tagNames.split(',').filter(Boolean) : []
}
