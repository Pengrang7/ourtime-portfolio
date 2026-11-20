import { useEffect, useRef, useState, type SyntheticEvent } from 'react'
import { createPortal } from 'react-dom'
import { useQuery } from '@tanstack/react-query'
import { previewPlace } from '../api/placeApi'
import { MapPlaceDetailTabs } from './MapPlaceDetailTabs'
import { directionsUrl } from './directions'
import type { MapSearchTarget } from './mapLocationSearch'

const NAVER_MAP_SCRIPT_ID = 'naver-map-script'
const DEFAULT_CENTER = { latitude: 37.5666103, longitude: 126.9783882 }
const SELECTED_ZOOM = 16
const SEARCH_ZOOM = 15

type LoadState = 'idle' | 'loading' | 'ready' | 'error'
type PlaceDetailTab = 'home' | 'menu' | 'photo'

function hideBrokenImage(event: SyntheticEvent<HTMLImageElement>) {
  const button = event.currentTarget.closest('button')
  if (button) button.hidden = true
  else event.currentTarget.hidden = true
}

function displayImageUrl(url: string) {
  try {
    if (new URL(url).hostname !== 'blogfiles.pstatic.net') return url
    return `https://search.pstatic.net/common/?autoRotate=true&type=w560_sharpen&src=${encodeURIComponent(url)}`
  } catch {
    return url
  }
}

interface NaverLatLng {
  readonly __latLng: unique symbol
}

interface NaverMapInstance {
  getZoom(): number
  setZoom(zoom: number, useEffect?: boolean): void
  panTo(latLng: NaverLatLng): void
  fitBounds(bounds: object, margin?: object): void
  morph(latLng: NaverLatLng, zoom: number): void
  setSize(size: { width: number; height: number }): void
}

interface NaverMarker {
  setMap(map: NaverMapInstance | null): void
  setZIndex(zIndex: number): void
  setIcon(icon: object): void
}

interface NaverMapsApi {
  LatLng: new (latitude: number, longitude: number) => NaverLatLng
  LatLngBounds: new (sw: NaverLatLng, ne: NaverLatLng) => { extend(latLng: NaverLatLng): void }
  Map: new (element: HTMLElement, options: { center: NaverLatLng; zoom: number }) => NaverMapInstance
  Marker: new (options: { map: NaverMapInstance; position: NaverLatLng; title: string; icon?: object }) => NaverMarker
  Point: new (x: number, y: number) => object
  Event: {
    addListener(target: object, event: string, listener: () => void): unknown
  }
}

declare global {
  interface Window {
    naver?: {
      maps: NaverMapsApi
    }
  }
}

let naverMapLoadPromise: Promise<void> | null = null

function loadNaverMapScript(clientId: string): Promise<void> {
  if (window.naver?.maps) return Promise.resolve()
  if (naverMapLoadPromise) return naverMapLoadPromise

  naverMapLoadPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(NAVER_MAP_SCRIPT_ID) as HTMLScriptElement | null
    const script = existingScript ?? document.createElement('script')

    script.addEventListener('load', () => resolve(), { once: true })
    script.addEventListener('error', () => {
      naverMapLoadPromise = null
      reject(new Error('NAVER 지도 스크립트를 불러오지 못했습니다.'))
    }, { once: true })

    if (!existingScript) {
      script.id = NAVER_MAP_SCRIPT_ID
      script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`
      script.async = true
      document.head.appendChild(script)
    }
  })

  return naverMapLoadPromise
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (char) => `&#${char.charCodeAt(0)};`)
}

// 앵커(0,0)를 핀 끝으로 두고, 선택된 마커에는 장소명 라벨을 함께 그린다.
function markerIcon(place: MapPlace, selected: boolean): object {
  const label = selected ? `<span class="map-info">${escapeHtml(place.name)}</span>` : ''
  const icon = `<span class="map-pin-icon">${escapeHtml(place.icon)}</span>`
  const color = place.color && /^#[0-9a-f]{6}$/i.test(place.color) ? ` style="background:${place.color}"` : ''
  return {
    content: `<div class="map-marker${selected ? ' selected' : ''}">${label}<span class="map-pin"${color}>${icon}</span></div>`,
    anchor: window.naver ? new window.naver.maps.Point(0, 0) : undefined,
  }
}

export interface MapPlace {
  id: number
  name: string
  icon: string
  latitude: number
  longitude: number
  country: string | null
  address: string
  category: string | null
  phone: string | null
  tagNames: string | null
  provider: string
  originalUrl: string
  color?: string
}

interface NaverMapProps {
  places: MapPlace[]
  selectedId: number | null
  onSelect: (id: number | null) => void
  searchTarget?: MapSearchTarget | null
}

export function NaverMap({ places, selectedId, onSelect, searchTarget }: NaverMapProps) {
  const shellRef = useRef<HTMLDivElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<NaverMapInstance | null>(null)
  const markersRef = useRef(new Map<number, NaverMarker>())
  const menuCategoryDragRef = useRef({ pointerId: -1, startX: 0, scrollLeft: 0, moved: false })
  const onSelectRef = useRef(onSelect)
  const [loadState, setLoadState] = useState<LoadState>('idle')
  const [activeTab, setActiveTab] = useState<PlaceDetailTab>('home')
  const [activeMenuCategory, setActiveMenuCategory] = useState('all')
  const [expandedMenuImageUrl, setExpandedMenuImageUrl] = useState<string | null>(null)
  const clientId = import.meta.env.VITE_NAVER_MAP_CLIENT_ID?.trim()
  const selectedPlace = places.find((place) => place.id === selectedId)
  const naverDetailsQuery = useQuery({
    queryKey: ['naver-place-details-v3', selectedPlace?.originalUrl],
    queryFn: () => previewPlace(selectedPlace!.originalUrl),
    enabled: selectedPlace?.provider === 'NAVER',
    staleTime: 5 * 60 * 1000,
    retry: 1,
  })
  const naverDetails = naverDetailsQuery.data?.naverDetails
  const menus = naverDetails?.menus ?? []
  const menuImageUrls = naverDetails?.menuImageUrls ?? []
  const imageUrls = naverDetails?.imageUrls ?? []
  const microReviews = naverDetails?.microReviews ?? []
  const conveniences = naverDetails?.conveniences ?? []
  const businessHours = naverDetails?.businessHours ?? []
  const menuCategories = [...new Set(menus
    .flatMap((menu) => menu.categories?.length ? menu.categories : [menu.category])
    .filter((category): category is string => Boolean(category)))]
  const selectedMenuCategory = activeMenuCategory === 'all' || activeMenuCategory === 'images' || menuCategories.includes(activeMenuCategory)
    ? activeMenuCategory
    : 'all'
  const visibleMenus = selectedMenuCategory === 'all'
    ? menus
    : menus.filter((menu) => (menu.categories?.length ? menu.categories : [menu.category]).includes(selectedMenuCategory))
  onSelectRef.current = onSelect

  useEffect(() => {
    setActiveTab('home')
    setActiveMenuCategory('all')
    setExpandedMenuImageUrl(null)
  }, [selectedId])

  useEffect(() => {
    if (!expandedMenuImageUrl) return

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setExpandedMenuImageUrl(null)
    }

    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [expandedMenuImageUrl])

  const placeSignature = places
    .map((place) => `${place.id}:${place.latitude}:${place.longitude}:${place.name}:${place.icon}:${place.color ?? ''}`)
    .join('|')

  // 지도 인스턴스는 한 번만 만든다.
  useEffect(() => {
    if (!clientId || !containerRef.current) return
    let cancelled = false
    setLoadState('loading')

    loadNaverMapScript(clientId)
      .then(() => {
        if (cancelled || !containerRef.current || !window.naver?.maps) return
        const { maps } = window.naver
        mapRef.current = new maps.Map(containerRef.current, {
          center: new maps.LatLng(DEFAULT_CENTER.latitude, DEFAULT_CENTER.longitude),
          zoom: 12,
        })
        setLoadState('ready')
      })
      .catch(() => {
        if (!cancelled) setLoadState('error')
      })

    return () => {
      cancelled = true
    }
  }, [clientId])

  // 목록 너비 조절·화면 회전 등으로 영역 크기가 바뀌면 지도 크기를 맞춘다.
  // setSize가 지도 요소에 px 크기를 직접 쓰므로, 지도 요소가 아닌 바깥 영역을 관찰한다.
  useEffect(() => {
    const shell = shellRef.current
    const map = mapRef.current
    if (loadState !== 'ready' || !shell || !map) return
    const observer = new ResizeObserver(([entry]) => {
      const { width, height } = entry.contentRect
      if (width > 0 && height > 0) map.setSize({ width, height })
    })
    observer.observe(shell)
    return () => observer.disconnect()
  }, [loadState])

  // 장소 목록이 바뀌면 마커를 다시 그리고 전체가 보이도록 맞춘다.
  useEffect(() => {
    const map = mapRef.current
    const maps = window.naver?.maps
    if (loadState !== 'ready' || !map || !maps) return

    markersRef.current.forEach((marker) => marker.setMap(null))
    markersRef.current.clear()

    places.forEach((place) => {
      const marker = new maps.Marker({
        map,
        position: new maps.LatLng(place.latitude, place.longitude),
        title: place.name,
        icon: markerIcon(place, false),
      })
      maps.Event.addListener(marker, 'click', () => onSelectRef.current(place.id))
      markersRef.current.set(place.id, marker)
    })

    if (places.length === 1) {
      map.morph(new maps.LatLng(places[0].latitude, places[0].longitude), 15)
    } else if (places.length > 1) {
      const first = new maps.LatLng(places[0].latitude, places[0].longitude)
      const bounds = new maps.LatLngBounds(first, first)
      places.forEach((place) => bounds.extend(new maps.LatLng(place.latitude, place.longitude)))
      map.fitBounds(bounds, { top: 60, right: 60, bottom: 60, left: 60 })
    }
    // placeSignature에는 이 effect가 사용하는 모든 장소 필드가 포함된다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadState, placeSignature])

  // 선택된 장소로 이동하고 마커를 강조한다.
  useEffect(() => {
    const map = mapRef.current
    const maps = window.naver?.maps
    if (loadState !== 'ready' || !map || !maps) return

    places.forEach((place) => {
      const marker = markersRef.current.get(place.id)
      marker?.setIcon(markerIcon(place, place.id === selectedId))
      marker?.setZIndex(place.id === selectedId ? 100 : 1)
    })
    const place = places.find((item) => item.id === selectedId)
    if (place) {
      map.morph(new maps.LatLng(place.latitude, place.longitude), Math.max(map.getZoom(), SELECTED_ZOOM))
    }
    // placeSignature에는 이 effect가 사용하는 모든 장소 필드가 포함된다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadState, selectedId, placeSignature])

  useEffect(() => {
    const map = mapRef.current
    const maps = window.naver?.maps
    if (loadState !== 'ready' || !map || !maps || !searchTarget) return
    map.morph(new maps.LatLng(searchTarget.latitude, searchTarget.longitude), SEARCH_ZOOM)
  }, [loadState, searchTarget, placeSignature])

  if (!clientId) {
    return (
      <p className="map-message">
        `frontend/.env.local`에 `VITE_NAVER_MAP_CLIENT_ID`를 설정하면 지도가 표시됩니다.
      </p>
    )
  }

  return (
    <div ref={shellRef} className="map-shell">
      <div ref={containerRef} className="naver-map" aria-label="NAVER 지도" />
      {selectedPlace && (
        <aside className="naver-place-card" aria-label={`${selectedPlace.name} 장소 정보`}>
          <header className="naver-place-header">
            <button
              type="button"
              className="naver-place-card-close"
              aria-label="장소 정보 닫기"
              onClick={() => onSelect(null)}
            >
              ×
            </button>
            <div className="naver-place-heading">
              <h2>{selectedPlace.name}</h2>
              {selectedPlace.category && <span>{selectedPlace.category}</span>}
            </div>
            <div className="naver-place-meta-row">
              {naverDetails?.visitorReviewScore != null && <><strong>★ {naverDetails.visitorReviewScore}</strong><i aria-hidden="true">|</i></>}
              <a href={selectedPlace.originalUrl} target="_blank" rel="noreferrer">원본</a>
              <i aria-hidden="true">|</i>
              <a
                className="naver-place-directions"
                href={directionsUrl({
                  latitude: selectedPlace.latitude,
                  longitude: selectedPlace.longitude,
                  name: selectedPlace.name,
                  mapType: 'DOMESTIC',
                })}
                target="_blank"
                rel="noreferrer"
              >길찾기</a>
            </div>
          </header>
          {imageUrls.length > 0 && (
            <div className="naver-place-photos">
              {imageUrls.slice(0, 3).map((url, index) => (
                <button
                  type="button"
                  key={url}
                  aria-label={`${selectedPlace.name} 대표 사진 ${index + 1} 확대`}
                  onClick={() => setExpandedMenuImageUrl(url)}
                >
                  <img src={displayImageUrl(url)} alt={`${selectedPlace.name} 사진 ${index + 1}`} loading="lazy" onError={hideBrokenImage} />
                </button>
              ))}
            </div>
          )}
          <MapPlaceDetailTabs<PlaceDetailTab>
            tabs={[
              { id: 'home', label: '홈' },
              { id: 'menu', label: `메뉴${menus.length ? ` ${menus.length}` : ''}` },
              { id: 'photo', label: `사진${imageUrls.length ? ` ${imageUrls.length}` : ''}` },
            ]}
            activeTab={activeTab}
            onChange={setActiveTab}
          >
            {activeTab === 'home' && (
              <>
              {naverDetailsQuery.isLoading && <p className="naver-place-loading">네이버 장소 정보를 불러오는 중입니다.</p>}
              {naverDetailsQuery.isError && <p className="naver-place-loading">네이버 상세 정보를 불러오지 못했습니다.</p>}
              {microReviews.map((review) => (
                <p className="naver-place-description" key={review}>“{review}”</p>
              ))}
              {naverDetails?.businessStatus && (
                <p className="naver-place-business-status">
                  <strong>{naverDetails.businessStatus}</strong>
                  {naverDetails.businessStatusDescription && <span>{naverDetails.businessStatusDescription}</span>}
                </p>
              )}
              <p className="naver-place-address"><span aria-hidden="true">●</span><strong>주소</strong>{selectedPlace.address}</p>
              {selectedPlace.phone && (
                <p className="naver-place-phone"><span aria-hidden="true">●</span><strong>전화</strong>{selectedPlace.phone}</p>
              )}
              {conveniences.length > 0 && (
                <p className="naver-place-conveniences">
                  <span aria-hidden="true">●</span><strong>편의</strong>{conveniences.join(' · ')}
                </p>
              )}
              {naverDetails && (naverDetails.businessStatus || businessHours.length > 0) && (
                <section className="naver-place-section">
                  <h3>영업시간</h3>
                  {naverDetails.businessStatus && (
                    <p className="naver-place-business-status">
                      <strong>{naverDetails.businessStatus}</strong>
                      {naverDetails.businessStatusDescription && <span>{naverDetails.businessStatusDescription}</span>}
                    </p>
                  )}
                  {businessHours.length > 0 && (
                    <dl className="naver-place-hours">
                      {businessHours.map((item) => (
                        <div key={item.day}>
                          <dt>{item.day}</dt>
                          <dd>{item.hours ?? item.description ?? '정보 없음'}</dd>
                        </div>
                      ))}
                    </dl>
                  )}
                </section>
              )}
              </>
            )}
            {activeTab === 'menu' && (
              <>
              {naverDetailsQuery.isLoading ? (
                <p className="naver-place-loading">메뉴를 불러오는 중입니다.</p>
              ) : naverDetails && (menus.length > 0 || menuImageUrls.length > 0) ? (
                <>
                  <div
                    className="naver-menu-categories"
                    aria-label="메뉴 분류"
                    onPointerDown={(event) => {
                      if (event.pointerType !== 'mouse' || event.button !== 0) return

                      const container = event.currentTarget
                      menuCategoryDragRef.current = {
                        pointerId: event.pointerId,
                        startX: event.clientX,
                        scrollLeft: container.scrollLeft,
                        moved: false,
                      }
                    }}
                    onPointerMove={(event) => {
                      const drag = menuCategoryDragRef.current
                      if (drag.pointerId !== event.pointerId) return

                      const distance = event.clientX - drag.startX
                      if (!drag.moved && Math.abs(distance) > 6) {
                        drag.moved = true
                        event.currentTarget.setPointerCapture(event.pointerId)
                        event.currentTarget.classList.add('dragging')
                      }
                      if (!drag.moved) return

                      event.currentTarget.scrollLeft = drag.scrollLeft - distance
                      event.preventDefault()
                    }}
                    onPointerUp={(event) => {
                      const drag = menuCategoryDragRef.current
                      if (drag.pointerId !== event.pointerId) return

                      if (event.currentTarget.hasPointerCapture(event.pointerId)) {
                        event.currentTarget.releasePointerCapture(event.pointerId)
                      }
                      event.currentTarget.classList.remove('dragging')
                      drag.pointerId = -1
                    }}
                    onPointerCancel={(event) => {
                      event.currentTarget.classList.remove('dragging')
                      menuCategoryDragRef.current.pointerId = -1
                      menuCategoryDragRef.current.moved = false
                    }}
                    onClickCapture={(event) => {
                      if (!menuCategoryDragRef.current.moved) return

                      event.preventDefault()
                      event.stopPropagation()
                      menuCategoryDragRef.current.moved = false
                    }}
                    onWheel={(event) => {
                      const container = event.currentTarget
                      const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY
                      const nextScrollLeft = Math.max(0, Math.min(container.scrollWidth - container.clientWidth, container.scrollLeft + delta))

                      if (nextScrollLeft !== container.scrollLeft) {
                        container.scrollLeft = nextScrollLeft
                        event.preventDefault()
                      }
                    }}
                  >
                    <button type="button" className={selectedMenuCategory === 'all' ? 'active' : ''} onClick={() => setActiveMenuCategory('all')}>
                      전체
                    </button>
                    {menuCategories.map((category) => (
                      <button type="button" key={category} className={selectedMenuCategory === category ? 'active' : ''} onClick={() => setActiveMenuCategory(category)}>
                        {category}
                      </button>
                    ))}
                    {menuImageUrls.length > 0 && (
                      <button type="button" className={selectedMenuCategory === 'images' ? 'active' : ''} onClick={() => setActiveMenuCategory('images')}>
                        메뉴판 이미지로 보기
                      </button>
                    )}
                  </div>
                  {selectedMenuCategory === 'images' ? (
                    <div className="naver-menu-board-images">
                      {menuImageUrls.map((url, index) => (
                        <button
                          type="button"
                          className="naver-menu-board-image-button"
                          key={`${url}-${index}`}
                          aria-label={`${selectedPlace.name} 메뉴판 ${index + 1} 확대`}
                          onClick={() => setExpandedMenuImageUrl(url)}
                        >
                          <img src={displayImageUrl(url)} alt={`${selectedPlace.name} 메뉴판 ${index + 1}`} loading="lazy" onError={hideBrokenImage} />
                        </button>
                      ))}
                    </div>
                  ) : (
                    visibleMenus.length > 0 ? (
                      <ul className="naver-place-menu-list">
                        {visibleMenus.map((menu, index) => (
                        <li key={`${menu.name}-${index}`}>
                          <div className="naver-place-menu-text">
                            <strong>{menu.name}</strong>
                            {menu.description && <span>{menu.description}</span>}
                            {menu.price && <b>{menu.price}</b>}
                          </div>
                          {menu.imageUrl && (
                            <button
                              type="button"
                              className="naver-place-menu-image-button"
                              aria-label={`${menu.name} 메뉴 사진 확대`}
                              onClick={() => setExpandedMenuImageUrl(menu.imageUrl)}
                            >
                              <img src={displayImageUrl(menu.imageUrl)} alt={`${menu.name} 메뉴`} loading="lazy" onError={hideBrokenImage} />
                            </button>
                          )}
                        </li>
                        ))}
                      </ul>
                    ) : <p className="naver-place-empty">이 분류에 등록된 메뉴가 없습니다.</p>
                  )}
                </>
              ) : <p className="naver-place-empty">등록된 메뉴 정보가 없습니다.</p>}
              </>
            )}
            {activeTab === 'photo' && (
              <>
              {naverDetailsQuery.isLoading ? (
                <p className="naver-place-loading">사진을 불러오는 중입니다.</p>
              ) : imageUrls.length > 0 ? (
                <div className="naver-place-photo-grid">
                  {imageUrls.map((url, index) => (
                    <button
                      type="button"
                      key={`${url}-${index}`}
                      aria-label={`${selectedPlace.name} 사진 ${index + 1} 확대`}
                      onClick={() => setExpandedMenuImageUrl(url)}
                    >
                      <img src={displayImageUrl(url)} alt={`${selectedPlace.name} 사진 ${index + 1}`} loading="lazy" onError={hideBrokenImage} />
                    </button>
                  ))}
                </div>
              ) : <p className="naver-place-empty">등록된 사진 정보가 없습니다.</p>}
              </>
            )}
          </MapPlaceDetailTabs>
        </aside>
      )}
      {expandedMenuImageUrl && selectedPlace && createPortal(
        <div
          className="naver-menu-image-viewer"
          role="dialog"
          aria-modal="true"
          aria-label={`${selectedPlace.name} 확대 이미지`}
          onClick={() => setExpandedMenuImageUrl(null)}
        >
          <button
            type="button"
            className="naver-menu-image-viewer-close"
            aria-label="확대 이미지 닫기"
            onClick={() => setExpandedMenuImageUrl(null)}
          >
            ×
          </button>
          <img
            src={displayImageUrl(expandedMenuImageUrl)}
            alt={`${selectedPlace.name} 확대 사진`}
            onClick={(event) => event.stopPropagation()}
            onError={() => setExpandedMenuImageUrl(null)}
          />
        </div>,
        document.body,
      )}
      {loadState === 'loading' && <p className="map-status">지도를 불러오는 중입니다.</p>}
      {loadState === 'error' && (
        <p className="map-status map-error">지도 인증키와 Web 서비스 URL을 확인해 주세요.</p>
      )}
    </div>
  )
}
