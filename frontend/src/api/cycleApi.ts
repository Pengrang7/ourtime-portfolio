import { apiClient } from './client'

export interface CyclePeriod {
  periodId: number
  startDate: string
  endDate: string
  createdAt: string
  updatedAt: string
}

export interface CyclePeriodInput {
  startDate: string
  endDate: string
}

export interface BoardCyclePeriod extends CyclePeriod {
  userId: number
  nickname: string
}

export async function fetchCyclePeriods(): Promise<CyclePeriod[]> {
  return (await apiClient.get<CyclePeriod[]>('/api/users/me/cycle-periods')).data
}

export async function fetchBoardCyclePeriods(boardId: number): Promise<BoardCyclePeriod[]> {
  return (await apiClient.get<BoardCyclePeriod[]>(`/api/boards/${boardId}/cycle-periods`)).data
}

export async function createCyclePeriod(input: CyclePeriodInput): Promise<CyclePeriod> {
  return (await apiClient.post<CyclePeriod>('/api/users/me/cycle-periods', input)).data
}

export async function updateCyclePeriod(periodId: number, input: CyclePeriodInput): Promise<CyclePeriod> {
  return (await apiClient.patch<CyclePeriod>(`/api/users/me/cycle-periods/${periodId}`, input)).data
}

export async function deleteCyclePeriod(periodId: number): Promise<void> {
  await apiClient.delete(`/api/users/me/cycle-periods/${periodId}`)
}
