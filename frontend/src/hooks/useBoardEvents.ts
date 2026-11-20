import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'

export function useBoardEvents(boardId: number | null) {
  const queryClient = useQueryClient()

  useEffect(() => {
    if (boardId === null) return

    const eventSource = new EventSource(`/api/boards/${boardId}/events`)
    const refreshSchedules = () => {
      queryClient.invalidateQueries({ queryKey: ['schedules', boardId] })
    }
    const refreshCyclePeriods = () => {
      queryClient.invalidateQueries({ queryKey: ['board-cycle-periods', boardId] })
    }
    const refreshPlaces = () => {
      queryClient.invalidateQueries({ queryKey: ['places', boardId] })
      refreshSchedules()
    }
    const refreshTags = () => {
      queryClient.invalidateQueries({ queryKey: ['tags', boardId] })
      refreshPlaces()
    }

    eventSource.addEventListener('places-changed', refreshPlaces)
    eventSource.addEventListener('schedules-changed', refreshSchedules)
    eventSource.addEventListener('cycle-periods-changed', refreshCyclePeriods)
    eventSource.addEventListener('tags-changed', refreshTags)
    return () => {
      eventSource.removeEventListener('places-changed', refreshPlaces)
      eventSource.removeEventListener('schedules-changed', refreshSchedules)
      eventSource.removeEventListener('cycle-periods-changed', refreshCyclePeriods)
      eventSource.removeEventListener('tags-changed', refreshTags)
      eventSource.close()
    }
  }, [boardId, queryClient])
}
