import type { GoogleMapsApi } from '../googleTypes'

const GOOGLE_MAP_SCRIPT_ID = 'google-map-script'
const GOOGLE_MAP_CALLBACK = '__placeBoardGoogleMapsReady'

export interface MapSearchTarget {
  latitude: number
  longitude: number
  request?: number
}

let googleMapLoadPromise: Promise<void> | null = null

export function loadGoogleMapScript(apiKey: string): Promise<void> {
  if (window.google?.maps) return Promise.resolve()
  if (googleMapLoadPromise) return googleMapLoadPromise

  googleMapLoadPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(GOOGLE_MAP_SCRIPT_ID) as HTMLScriptElement | null
    const script = existingScript ?? document.createElement('script')

    window[GOOGLE_MAP_CALLBACK] = () => {
      delete window[GOOGLE_MAP_CALLBACK]
      resolve()
    }
    script.addEventListener('error', () => {
      googleMapLoadPromise = null
      delete window[GOOGLE_MAP_CALLBACK]
      reject(new Error('Google 지도 스크립트를 불러오지 못했습니다.'))
    }, { once: true })

    if (!existingScript) {
      script.id = GOOGLE_MAP_SCRIPT_ID
      script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&loading=async&callback=${GOOGLE_MAP_CALLBACK}&language=ko&region=KR&v=weekly&auth_referrer_policy=origin`
      script.async = true
      document.head.appendChild(script)
    }
  })

  return googleMapLoadPromise
}

export async function searchMapLocation(query: string, apiKey: string): Promise<MapSearchTarget | null> {
  await loadGoogleMapScript(apiKey)
  const maps = window.google?.maps as GoogleMapsApi | undefined
  if (!maps) throw new Error('Google 지도 API를 사용할 수 없습니다.')

  const { Place } = await maps.importLibrary('places')
  const result = await Place.searchByText({
    textQuery: query,
    fields: ['location'],
    language: 'ko',
    region: 'KR',
    maxResultCount: 1,
  })
  const location = result.places[0]?.location
  if (!location) return null

  return { latitude: location.lat(), longitude: location.lng() }
}
