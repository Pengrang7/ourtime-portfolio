import { apiClient } from './client'

/** Google Calendar 일정 색 11가지와 같다. 순서는 Google colorId(1~11)가 아니라 화면에 보여 줄 순서다. */
export const SCHEDULE_COLORS = [
  '#0B8043', '#33B679', '#039BE5', '#3F51B5', '#7986CB', '#8E24AA',
  '#D50000', '#E67C73', '#F4511E', '#F6BF26', '#616161',
] as const

export const SCHEDULE_COLOR_LABELS: Record<string, string> = {
  '#0B8043': '바질', '#33B679': '세이지', '#039BE5': '공작', '#3F51B5': '블루베리', '#7986CB': '라벤더',
  '#8E24AA': '포도', '#D50000': '토마토', '#E67C73': '플라밍고', '#F4511E': '귤', '#F6BF26': '바나나',
  '#616161': '흑연',
}

export interface Schedule {
  scheduleId: number
  boardId: number
  boardPlaceId: number | null
  title?: string | null
  placeName: string | null
  placeAddress: string | null
  latitude: number | null
  longitude: number | null
  startAt: string
  /** 종일 일정이면 마지막 날의 다음 날 0시(배타적) */
  endAt: string
  allDay: boolean
  memo: string | null
  color: string
  tagIds: string | null
  tagNames: string | null
  tagIcons: string | null
  createdBy: number
  createdByNickname: string
  createdAt: string
  updatedAt: string
}

export interface SchedulePayload {
  boardPlaceId: number | null
  title: string
  startAt: string
  endAt: string
  allDay: boolean
  memo: string
  tagIds: number[]
  color: string
}

export async function fetchSchedules(boardId: number, from: string, to: string): Promise<Schedule[]> {
  return (await apiClient.get<Schedule[]>(`/api/boards/${boardId}/schedules`, {
    params: { from, to },
  })).data
}

export async function createSchedule(boardId: number, payload: SchedulePayload): Promise<Schedule> {
  return (await apiClient.post<Schedule>(`/api/boards/${boardId}/schedules`, payload)).data
}

export async function updateSchedule(
  boardId: number,
  scheduleId: number,
  payload: SchedulePayload,
): Promise<Schedule> {
  return (await apiClient.patch<Schedule>(`/api/boards/${boardId}/schedules/${scheduleId}`, payload)).data
}

export async function deleteSchedule(boardId: number, scheduleId: number): Promise<void> {
  await apiClient.delete(`/api/boards/${boardId}/schedules/${scheduleId}`)
}
