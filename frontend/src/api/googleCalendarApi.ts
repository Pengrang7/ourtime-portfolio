import { apiClient } from './client'

export interface GoogleCalendarStatus {
  configured: boolean
  connected: boolean
}

export async function fetchGoogleCalendarStatus(): Promise<GoogleCalendarStatus> {
  return (await apiClient.get<GoogleCalendarStatus>('/api/google-calendar/status')).data
}

export async function disconnectGoogleCalendar(): Promise<void> {
  await apiClient.delete('/api/google-calendar/connection')
}
