export interface GoogleIdentityApi {
  initialize(options: { client_id: string; callback: (response: { credential: string }) => void }): void
  renderButton(element: HTMLElement, options: Record<string, string | number>): void
  disableAutoSelect(): void
}

export type GoogleLatLng = { lat: number; lng: number }

export interface GoogleMapInstance {
  fitBounds(bounds: GoogleLatLngBounds, padding?: number): void
  getZoom(): number | undefined
  panTo(position: GoogleLatLng): void
  setZoom(zoom: number): void
}

export interface GoogleLatLngBounds {
  extend(position: GoogleLatLng): void
}

export interface GoogleMarker {
  addListener(event: string, listener: () => void): unknown
  setIcon(icon: object): void
  setLabel(label: object): void
  setMap(map: GoogleMapInstance | null): void
  setZIndex(zIndex: number): void
}

export interface GoogleInfoWindow {
  close(): void
  open(options: { map: GoogleMapInstance; anchor: GoogleMarker }): void
  setContent(content: string): void
}

export interface GoogleMapsApi {
  Map: new (element: HTMLElement, options: { center: GoogleLatLng; zoom: number; mapTypeControl: boolean }) => GoogleMapInstance
  Marker: new (options: {
    map: GoogleMapInstance
    position: GoogleLatLng
    title: string
    icon: object
    label: object
  }) => GoogleMarker
  LatLngBounds: new () => GoogleLatLngBounds
  InfoWindow: new () => GoogleInfoWindow
  Point: new (x: number, y: number) => object
  SymbolPath: { CIRCLE: unknown }
  event: { trigger(target: object, event: string): void }
  importLibrary(name: 'places'): Promise<GooglePlacesLibrary>
}

export interface GooglePhotoAttribution {
  displayName: string
  uri: string | null
}

export interface GooglePlacePhoto {
  authorAttributions: GooglePhotoAttribution[]
  getURI(options: { maxHeight?: number; maxWidth?: number }): string
}

export interface GooglePlaceDetailsInstance {
  photos?: GooglePlacePhoto[]
  fetchFields(options: { fields: string[] }): Promise<void>
}

export interface GooglePlaceLocation {
  lat(): number
  lng(): number
}

export interface GooglePlaceSearchResult {
  location?: GooglePlaceLocation
}

export interface GooglePlaceConstructor {
  new (options: { id: string }): GooglePlaceDetailsInstance
  searchByText(options: {
    textQuery: string
    fields: string[]
    language: string
    region: string
    maxResultCount: number
  }): Promise<{ places: GooglePlaceSearchResult[] }>
}

export interface GooglePlacesLibrary {
  Place: GooglePlaceConstructor
}

export interface GoogleGlobal {
  accounts?: { id: GoogleIdentityApi }
  maps?: GoogleMapsApi
}
