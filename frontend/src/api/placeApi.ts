import { apiClient, getApiErrorMessage } from './client'

export interface PlacePreview {
  provider: string
  providerPlaceId: string | null
  originalUrl: string
  name: string
  address: string
  latitude: number | null
  longitude: number | null
  country: string | null
  sido: string | null
  sigungu: string | null
  dong: string | null
  category: string | null
  phone: string | null
  naverDetails: NaverPlaceDetails | null
  googleDetails: GooglePlaceDetails | null
}

export interface GooglePlaceDetails {
  googlePlaceId: string | null
  rating: number | null
  reviewsTotal: number | null
  description: string | null
  businessStatus: string | null
  businessStatusDescription: string | null
  businessHours: NaverBusinessHour[]
  websiteUrl: string | null
}

export interface NaverPlaceDetails {
  imageUrls: string[]
  visitorReviewsTotal: number | null
  blogReviewsTotal: number | null
  visitorReviewScore: number | null
  microReviews: string[]
  conveniences: string[]
  businessStatus: string | null
  businessStatusDescription: string | null
  businessHours: NaverBusinessHour[]
  menus: NaverMenuItem[]
  menuImageUrls?: string[]
}

export interface NaverBusinessHour {
  day: string
  hours: string | null
  description: string | null
}

export interface NaverMenuItem {
  name: string
  price: string | null
  description: string | null
  imageUrl: string | null
  category: string | null
  categories?: string[]
}

export type RevisitIntention = 'YES' | 'MAYBE' | 'NO'
export type RevisitFilter = RevisitIntention | 'UNREVIEWED'

export interface BoardPlace {
  boardPlaceId: number
  placeId: number
  name: string
  address: string
  latitude: number | null
  longitude: number | null
  country: string | null
  sido: string | null
  sigungu: string | null
  dong: string | null
  category: string | null
  phone: string | null
  provider: string
  providerPlaceId: string | null
  originalUrl: string
  sourceStatus: 'ACTIVE' | 'UNAVAILABLE' | 'MOVED' | 'CHECK_REQUIRED'
  lastVerifiedAt: string | null
  revisitYesCount: number
  revisitMaybeCount: number
  revisitNoCount: number
  createdBy: number
  createdByNickname: string
  createdAt: string
  tagNames: string | null
}

export interface PlaceRegionOption {
  sido: string
  sigungu: string | null
}

export interface PlaceSavePayload extends PlacePreview {
  tagIds: number[]
}

export interface PlaceReview {
  reviewId: number
  boardPlaceId: number
  userId: number
  nickname: string
  revisitIntention: RevisitIntention
  content: string | null
  visitedOn: string | null
  createdAt: string
  updatedAt: string
}

export interface PlaceReviewPayload {
  revisitIntention: RevisitIntention
  content: string
  visitedOn: string | null
}

export async function previewPlace(url: string): Promise<PlacePreview> {
  const response = await apiClient.post<PlacePreview>('/api/places/preview', { url })
  return response.data
}

export async function fetchPlaces(
  boardId: number,
  filters: { revisit?: RevisitFilter; sido?: string; sigungu?: string; tagId?: number; keyword?: string },
): Promise<BoardPlace[]> {
  return (await apiClient.get<BoardPlace[]>(`/api/boards/${boardId}/places`, { params: filters })).data
}

export async function fetchPlaceRegions(boardId: number): Promise<PlaceRegionOption[]> {
  return (await apiClient.get<PlaceRegionOption[]>(`/api/boards/${boardId}/places/regions`)).data
}

export async function savePlace(boardId: number, payload: PlaceSavePayload): Promise<BoardPlace> {
  const { naverDetails: _naverDetails, googleDetails: _googleDetails, ...savePayload } = payload
  return (await apiClient.post<BoardPlace>(`/api/boards/${boardId}/places`, savePayload)).data
}

export async function updateBoardPlace(
  boardId: number,
  boardPlaceId: number,
  tagIds: number[],
): Promise<void> {
  await apiClient.patch(`/api/boards/${boardId}/places/${boardPlaceId}`, { tagIds })
}

export async function refreshBoardPlace(boardId: number, boardPlaceId: number): Promise<BoardPlace> {
  return (await apiClient.post<BoardPlace>(`/api/boards/${boardId}/places/${boardPlaceId}/refresh`)).data
}

export async function fetchPlaceReviews(boardId: number, boardPlaceId: number): Promise<PlaceReview[]> {
  return (await apiClient.get<PlaceReview[]>(`/api/boards/${boardId}/places/${boardPlaceId}/reviews`)).data
}

export async function saveMyPlaceReview(
  boardId: number,
  boardPlaceId: number,
  payload: PlaceReviewPayload,
): Promise<PlaceReview> {
  return (await apiClient.put<PlaceReview>(`/api/boards/${boardId}/places/${boardPlaceId}/reviews/me`, payload)).data
}

export async function deleteMyPlaceReview(boardId: number, boardPlaceId: number): Promise<void> {
  await apiClient.delete(`/api/boards/${boardId}/places/${boardPlaceId}/reviews/me`)
}

export async function deleteBoardPlace(boardId: number, boardPlaceId: number): Promise<void> {
  await apiClient.delete(`/api/boards/${boardId}/places/${boardPlaceId}`)
}

export { getApiErrorMessage }
