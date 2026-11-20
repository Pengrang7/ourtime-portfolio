import { apiClient } from './client'

export interface Holiday {
  date: string
  name: string
}

export async function fetchHolidays(from: string, to: string): Promise<Holiday[]> {
  return (await apiClient.get<Holiday[]>('/api/holidays', { params: { from, to } })).data
}
