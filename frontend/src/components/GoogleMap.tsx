import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useQuery } from '@tanstack/react-query'
import { previewPlace } from '../api/placeApi'
import type {
  GoogleGlobal,
  GoogleInfoWindow,
  GoogleMapInstance,
  GoogleMapsApi,
  GoogleMarker,
} from '../googleTypes'
import { directionsUrl } from './directions'
import { MapPlaceDetailTabs } from './MapPlaceDetailTabs'
import type { MapPlace } from './NaverMap'
import { loadGoogleMapScript, type MapSearchTarget } from './mapLocationSearch'

const DEFAULT_CENTER = { lat: 37.5666103, lng: 126.9783882 }
const SELECTED_ZOOM = 16
const SEARCH_ZOOM = 15

type LoadState = 'idle' | 'loading' | 'ready' | 'error'
type GooglePlaceTab = 'home' | 'menu' | 'photo'
type GooglePhoto = { url: string; attribution: string; attributionUrl: string | null }

function GooglePhotoGrid({
  photos,
  placeName,
  onPhotoClick,
}: {
  photos: GooglePhoto[]
  placeName: string
  onPhotoClick: (photo: GooglePhoto) => void
}) {
  return (
    <div className="google-place-photo-grid">
      {photos.map((photo, index) => (
        <figure key={photo.url}>
          <button type="button" aria-label={`${placeName} 사진 ${index + 1} 확대`} onClick={() => onPhotoClick(photo)}>
            <img src={photo.url} alt={`${placeName} 사진 ${index + 1}`} loading="lazy" />
          </button>
          <figcaption>
            {photo.attributionUrl
              ? <a href={photo.attributionUrl} target="_blank" rel="noreferrer">{photo.attribution}</a>
              : photo.attribution}
          </figcaption>
        </figure>
      ))}
    </div>
  )
}

declare global {
  interface Window {
    google?: GoogleGlobal
    __placeBoardGoogleMapsReady?: () => void
    gm_authFailure?: () => void
  }
}

function markerIcon(maps: GoogleMapsApi, place: MapPlace, selected: boolean): object {
  return {
    path: maps.SymbolPath.CIRCLE,
    fillColor: place.color ?? (selected ? '#e8590c' : '#168a4a'),
    fillOpacity: 1,
    strokeColor: '#ffffff',
    strokeWeight: 3,
    scale: selected ? 20 : 16,
    labelOrigin: new maps.Point(0, 0),
  }
}

function markerLabel(place: MapPlace, selected: boolean): object {
  return {
    text: place.icon,
    fontSize: selected ? '20px' : '16px',
    fontWeight: '400',
  }
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (char) => `&#${char.charCodeAt(0)};`)
}

interface GoogleMapProps {
  places: MapPlace[]
  selectedId: number | null
  onSelect: (id: number | null) => void
  searchTarget?: MapSearchTarget | null
}

export function GoogleMap({ places, selectedId, onSelect, searchTarget }: GoogleMapProps) {
  const shellRef = useRef<HTMLDivElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<GoogleMapInstance | null>(null)
  const markersRef = useRef(new globalThis.Map<number, GoogleMarker>())
  const infoWindowRef = useRef<GoogleInfoWindow | null>(null)
  const onSelectRef = useRef(onSelect)
  const [loadState, setLoadState] = useState<LoadState>('idle')
  const [activeTab, setActiveTab] = useState<GooglePlaceTab>('home')
  const [photos, setPhotos] = useState<GooglePhoto[]>([])
  const [photoState, setPhotoState] = useState<LoadState>('idle')
  const [expandedPhoto, setExpandedPhoto] = useState<GooglePhoto | null>(null)
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY?.trim()
  const selectedPlace = places.find((place) => place.id === selectedId)
  const googleDetailsQuery = useQuery({
    queryKey: ['google-place-details-v1', selectedPlace?.originalUrl],
    queryFn: () => previewPlace(selectedPlace!.originalUrl),
    enabled: selectedPlace?.provider === 'GOOGLE',
    staleTime: 5 * 60 * 1000,
    retry: 1,
  })
  const googleDetails = googleDetailsQuery.data?.googleDetails
  const isFoodPlace = /음식|식당|요리|레스토랑|카페|커피|베이커리|제과|디저트|이자카야|주점|선술집|포장마차|바|술집|펍|호프|뷔페|피자|치킨|햄버거|스시|초밥|라멘|우동|소바|restaurant|cafe|coffee|bakery|bar|pub|food|pizza|sushi|ramen|izakaya/i.test(selectedPlace?.category ?? '')
  onSelectRef.current = onSelect

  useEffect(() => {
    setActiveTab('home')
    setPhotos([])
    setPhotoState('idle')
    setExpandedPhoto(null)
  }, [selectedId])

  useEffect(() => {
    if (!expandedPhoto) return

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setExpandedPhoto(null)
    }

    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [expandedPhoto])

  useEffect(() => {
    const maps = window.google?.maps
    const googlePlaceId = googleDetails?.googlePlaceId
    if (loadState !== 'ready' || !maps || !googlePlaceId) return

    let cancelled = false
    setPhotoState('loading')
    maps.importLibrary('places')
      .then(async ({ Place }) => {
        const place = new Place({ id: googlePlaceId })
        await place.fetchFields({ fields: ['photos'] })
        if (cancelled) return
        setPhotos((place.photos ?? []).slice(0, 10).map((photo) => ({
          url: photo.getURI({ maxWidth: 1920, maxHeight: 1920 }),
          attribution: photo.authorAttributions[0]?.displayName ?? 'Google Maps 사용자',
          attributionUrl: photo.authorAttributions[0]?.uri ?? null,
        })))
        setPhotoState('ready')
      })
      .catch(() => {
        if (!cancelled) setPhotoState('error')
      })

    return () => { cancelled = true }
  }, [loadState, googleDetails?.googlePlaceId])

  const placeSignature = places
    .map((place) => `${place.id}:${place.latitude}:${place.longitude}:${place.name}:${place.icon}:${place.color ?? ''}`)
    .join('|')

  useEffect(() => {
    if (!apiKey || !containerRef.current) return
    let cancelled = false
    const markers = markersRef.current
    setLoadState('loading')
    window.gm_authFailure = () => setLoadState('error')

    loadGoogleMapScript(apiKey)
      .then(() => {
        if (cancelled || !containerRef.current || !window.google?.maps) return
        const { maps } = window.google
        mapRef.current = new maps.Map(containerRef.current, {
          center: DEFAULT_CENTER,
          zoom: 3,
          mapTypeControl: false,
        })
        infoWindowRef.current = new maps.InfoWindow()
        setLoadState('ready')
      })
      .catch(() => {
        if (!cancelled) setLoadState('error')
      })

    return () => {
      cancelled = true
      markers.forEach((marker) => marker.setMap(null))
      markers.clear()
      infoWindowRef.current?.close()
      mapRef.current = null
      delete window.gm_authFailure
    }
  }, [apiKey])

  useEffect(() => {
    const shell = shellRef.current
    const map = mapRef.current
    const maps = window.google?.maps
    if (loadState !== 'ready' || !shell || !map || !maps) return
    const observer = new ResizeObserver(() => maps.event.trigger(map, 'resize'))
    observer.observe(shell)
    return () => observer.disconnect()
  }, [loadState])

  useEffect(() => {
    const map = mapRef.current
    const maps = window.google?.maps
    if (loadState !== 'ready' || !map || !maps) return

    markersRef.current.forEach((marker) => marker.setMap(null))
    markersRef.current.clear()
    infoWindowRef.current?.close()

    const bounds = new maps.LatLngBounds()
    places.forEach((place) => {
      const position = { lat: place.latitude, lng: place.longitude }
      const marker = new maps.Marker({
        map,
        position,
        title: place.name,
        icon: markerIcon(maps, place, false),
        label: markerLabel(place, false),
      })
      marker.addListener('click', () => onSelectRef.current(place.id))
      markersRef.current.set(place.id, marker)
      bounds.extend(position)
    })

    if (places.length === 1) {
      map.panTo({ lat: places[0].latitude, lng: places[0].longitude })
      map.setZoom(15)
    } else if (places.length > 1) {
      map.fitBounds(bounds, 60)
    }
    // placeSignature에는 이 effect가 사용하는 모든 장소 필드가 포함된다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadState, placeSignature])

  useEffect(() => {
    const map = mapRef.current
    const maps = window.google?.maps
    if (loadState !== 'ready' || !map || !maps) return

    places.forEach((place) => {
      const selected = place.id === selectedId
      const marker = markersRef.current.get(place.id)
      marker?.setIcon(markerIcon(maps, place, selected))
      marker?.setLabel(markerLabel(place, selected))
      marker?.setZIndex(selected ? 100 : 1)
    })

    const place = places.find((item) => item.id === selectedId)
    const marker = place ? markersRef.current.get(place.id) : undefined
    if (place && marker) {
      map.panTo({ lat: place.latitude, lng: place.longitude })
      map.setZoom(Math.max(map.getZoom() ?? 0, SELECTED_ZOOM))
      infoWindowRef.current?.setContent(`<strong>${escapeHtml(place.name)}</strong>`)
      infoWindowRef.current?.open({ map, anchor: marker })
    } else {
      infoWindowRef.current?.close()
    }
    // placeSignature에는 이 effect가 사용하는 모든 장소 필드가 포함된다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadState, selectedId, placeSignature])

  useEffect(() => {
    const map = mapRef.current
    if (loadState !== 'ready' || !map || !searchTarget) return
    map.panTo({ lat: searchTarget.latitude, lng: searchTarget.longitude })
    map.setZoom(SEARCH_ZOOM)
  }, [loadState, searchTarget, placeSignature])

  if (!apiKey) {
    return (
      <p className="map-message">
        `frontend/.env.local`에 `VITE_GOOGLE_MAPS_API_KEY`를 설정하면 해외 지도가 표시됩니다.
      </p>
    )
  }

  return (
    <div ref={shellRef} className="map-shell">
      <div ref={containerRef} className="google-map" aria-label="Google 지도" />
      {selectedPlace && (
        <aside className="google-place-card" aria-label={`${selectedPlace.name} 장소 정보`}>
          <header className="google-place-header">
            <button
              type="button"
              className="google-place-card-close"
              aria-label="장소 정보 닫기"
              onClick={() => onSelect(null)}
            >
              ×
            </button>
            <div className="google-place-heading">
              <span className="google-place-icon" aria-hidden="true">{selectedPlace.icon}</span>
              <div>
                <h2>{selectedPlace.name}</h2>
                {selectedPlace.category && <span>{selectedPlace.category}</span>}
              </div>
            </div>
            {selectedPlace.tagNames && (
              <p className="google-place-tags">{selectedPlace.tagNames.split(',').join(' · ')}</p>
            )}
            {googleDetails && (googleDetails.rating !== null || googleDetails.reviewsTotal !== null) && (
              <p className="google-place-reviews">
                {googleDetails.rating !== null && <strong>★ {googleDetails.rating}</strong>}
                {googleDetails.reviewsTotal !== null && <span>리뷰 {googleDetails.reviewsTotal.toLocaleString()}</span>}
              </p>
            )}
          </header>
          {photos.length > 0 && (
            <div className="google-place-photos">
              {photos.slice(0, 3).map((photo, index) => (
                <figure key={photo.url}>
                  <button type="button" aria-label={`${selectedPlace.name} 사진 ${index + 1} 확대`} onClick={() => setExpandedPhoto(photo)}>
                    <img src={photo.url} alt={`${selectedPlace.name} 사진 ${index + 1}`} loading="lazy" />
                  </button>
                  <figcaption>
                    {photo.attributionUrl
                      ? <a href={photo.attributionUrl} target="_blank" rel="noreferrer">{photo.attribution}</a>
                      : photo.attribution}
                  </figcaption>
                </figure>
              ))}
            </div>
          )}
          <MapPlaceDetailTabs<GooglePlaceTab>
            tabs={[
              { id: 'home', label: '홈' },
              ...(isFoodPlace ? [{ id: 'menu' as const, label: '메뉴' }] : []),
              { id: 'photo', label: `사진${photos.length ? ` ${photos.length}` : ''}` },
            ]}
            activeTab={activeTab}
            onChange={setActiveTab}
          >
            {activeTab === 'home' && (
              <>
                {googleDetailsQuery.isLoading && <p className="google-place-loading">Google 장소 정보를 불러오는 중입니다.</p>}
                {googleDetailsQuery.isError && <p className="google-place-loading">Google 상세 정보를 불러오지 못했습니다.</p>}
                {googleDetails?.description && <p className="google-place-description">{googleDetails.description}</p>}
                {googleDetails?.businessStatus && (
                  <p className="google-place-business-status">
                    <strong>{googleDetails.businessStatus}</strong>
                    {googleDetails.businessStatusDescription && <span>{googleDetails.businessStatusDescription}</span>}
                  </p>
                )}
                <dl className="google-place-details">
                  <div>
                    <dt>주소</dt>
                    <dd>{selectedPlace.address}</dd>
                  </div>
                  {(selectedPlace.phone || googleDetailsQuery.data?.phone) && (
                    <div>
                      <dt>전화</dt>
                      <dd><a href={`tel:${selectedPlace.phone ?? googleDetailsQuery.data?.phone}`}>{selectedPlace.phone ?? googleDetailsQuery.data?.phone}</a></dd>
                    </div>
                  )}
                  {selectedPlace.country && (
                    <div>
                      <dt>국가</dt>
                      <dd>{selectedPlace.country}</dd>
                    </div>
                  )}
                </dl>
                {googleDetails && googleDetails.businessHours.length > 0 && (
                  <section className="google-place-section">
                    <h3>영업시간</h3>
                    <dl className="google-place-hours">
                      {googleDetails.businessHours.map((item) => (
                        <div key={item.day}>
                          <dt>{item.day}</dt>
                          <dd>{item.hours ?? item.description ?? '정보 없음'}</dd>
                        </div>
                      ))}
                    </dl>
                  </section>
                )}
                <div className="google-place-actions">
                  <a
                    className="google-place-primary-action"
                    href={directionsUrl({
                      latitude: selectedPlace.latitude,
                      longitude: selectedPlace.longitude,
                      name: selectedPlace.name,
                      mapType: 'OVERSEAS',
                    })}
                    target="_blank"
                    rel="noreferrer"
                  >길찾기</a>
                  {googleDetails?.websiteUrl && (
                    <a href={googleDetails.websiteUrl} target="_blank" rel="noreferrer">공식 웹사이트 ↗</a>
                  )}
                  <a href={selectedPlace.originalUrl} target="_blank" rel="noreferrer">Google 지도에서 보기 ↗</a>
                </div>
              </>
            )}
            {activeTab === 'menu' && (
              <div className="google-place-menu">
                <p className="google-place-photo-note">
                  Google Places API에서는 메뉴판 사진을 일반 장소 사진과 구분해 제공하지 않습니다.
                </p>
                {googleDetails?.websiteUrl && (
                  <a className="google-place-primary-action" href={googleDetails.websiteUrl} target="_blank" rel="noreferrer">
                    공식 웹사이트에서 메뉴 확인 ↗
                  </a>
                )}
                <a className="google-place-primary-action" href={selectedPlace.originalUrl} target="_blank" rel="noreferrer">
                  Google 지도에서 실제 메뉴 사진 보기 ↗
                </a>
              </div>
            )}
            {activeTab === 'photo' && (
              <>
                {photoState === 'loading' && <p className="google-place-loading">장소 사진을 불러오는 중입니다.</p>}
                {photoState === 'error' && <p className="google-place-loading">Places API를 확인해 주세요.</p>}
                {photoState === 'ready' && photos.length === 0 && <p className="google-place-empty">등록된 사진이 없습니다.</p>}
                {photos.length > 0 && <GooglePhotoGrid photos={photos} placeName={selectedPlace.name} onPhotoClick={setExpandedPhoto} />}
              </>
            )}
          </MapPlaceDetailTabs>
        </aside>
      )}
      {expandedPhoto && selectedPlace && createPortal(
        <div
          className="google-photo-image-viewer"
          role="dialog"
          aria-modal="true"
          aria-label={`${selectedPlace.name} 사진 확대`}
          onClick={() => setExpandedPhoto(null)}
        >
          <button
            type="button"
            className="google-photo-image-viewer-close"
            aria-label="확대 이미지 닫기"
            onClick={() => setExpandedPhoto(null)}
          >
            ×
          </button>
          <img
            src={expandedPhoto.url}
            alt={`${selectedPlace.name} 확대 사진`}
            onClick={(event) => event.stopPropagation()}
            onError={() => setExpandedPhoto(null)}
          />
        </div>,
        document.body,
      )}
      {loadState === 'loading' && <p className="map-status">Google 지도를 불러오는 중입니다.</p>}
      {loadState === 'error' && (
        <p className="map-status map-error">Google Maps API 키와 허용 도메인을 확인해 주세요.</p>
      )}
    </div>
  )
}
