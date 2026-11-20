import { FormEvent, useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Tag, createTag, deleteTag, updateTag } from '../api/boardApi'
import { getApiErrorMessage } from '../api/client'
import { fetchPlaces, updateBoardPlace } from '../api/placeApi'
import { Modal } from './Modal'
import { TAG_ICON_CHOICES, iconFor } from './placeIcons'
import {
  FALLBACK_SOURCE_TAGS,
  FALLBACK_TARGET_TAGS,
  classifyFallbackRestaurant,
  mergeFallbackRestaurantTag,
} from './restaurantClassification'

interface TagManageModalProps {
  boardId: number
  tags: Tag[]
  onClose: () => void
  onDeleted: (tagId: number) => void
}

interface TagDraft {
  tagId: number
  name: string
  icon: string | null
}

export function TagManageModal({ boardId, tags, onClose, onDeleted }: TagManageModalProps) {
  const queryClient = useQueryClient()
  const [newTagName, setNewTagName] = useState('')
  // undefined면 아직 고르지 않은 상태라 이름으로 추천한 아이콘을 쓴다.
  const [newTagIcon, setNewTagIcon] = useState<string | null | undefined>(undefined)
  const [editing, setEditing] = useState<TagDraft | null>(null)
  const [classificationResult, setClassificationResult] = useState<{ message: string, failed: boolean } | null>(null)
  const effectiveNewIcon = newTagIcon === undefined ? iconFor(newTagName) : newTagIcon

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['tags', boardId] })
    queryClient.invalidateQueries({ queryKey: ['places', boardId] })
    queryClient.invalidateQueries({ queryKey: ['schedules', boardId] })
  }
  const createMutation = useMutation({
    mutationFn: () => createTag(boardId, newTagName.trim(), effectiveNewIcon),
    onSuccess: () => {
      setNewTagName('')
      setNewTagIcon(undefined)
      refresh()
    },
  })
  const updateMutation = useMutation({
    mutationFn: ({ tagId, name, icon }: TagDraft) => updateTag(boardId, tagId, name, icon),
    onSuccess: () => {
      setEditing(null)
      refresh()
    },
  })
  const deleteMutation = useMutation({
    mutationFn: (tagId: number) => deleteTag(boardId, tagId),
    onSuccess: (_, tagId) => {
      onDeleted(tagId)
      refresh()
    },
  })
  const classifyMutation = useMutation({
    mutationFn: async () => {
      const tagsByName = new Map(tags.map((tag) => [tag.name, tag]))
      const requiredNames = [...FALLBACK_TARGET_TAGS]
      const missing = requiredNames.filter((name) => !tagsByName.has(name))
      if (missing.length > 0) throw new Error(`필요한 태그가 없습니다: ${missing.join(', ')}`)

      const placesById = new Map<number, Awaited<ReturnType<typeof fetchPlaces>>[number]>()
      for (const sourceName of FALLBACK_SOURCE_TAGS) {
        const sourceTag = tagsByName.get(sourceName)
        if (!sourceTag) continue
        const sourcePlaces = await fetchPlaces(boardId, { tagId: sourceTag.tagId })
        sourcePlaces.forEach((place) => placesById.set(place.boardPlaceId, place))
      }

      const counts = new Map<string, number>()
      const failures: string[] = []
      let updated = 0

      // 같은 보드의 태그를 동시에 갱신하면 DB 잠금 충돌이 날 수 있어 순차 처리한다.
      for (const place of placesById.values()) {
        const classification = classifyFallbackRestaurant(place)
        const currentNames = (place.tagNames ?? '').split(',').filter(Boolean)
        const nextNames = mergeFallbackRestaurantTag(currentNames, classification)
        const nextTagIds = [...new Set(nextNames.map((name) => tagsByName.get(name)?.tagId).filter((id): id is number => id !== undefined))]
        const currentTagIds = currentNames.map((name) => tagsByName.get(name)?.tagId).filter((id): id is number => id !== undefined)
        const unchanged = nextTagIds.length === currentTagIds.length
          && nextTagIds.every((id) => currentTagIds.includes(id))
        counts.set(classification, (counts.get(classification) ?? 0) + 1)
        if (unchanged) continue
        try {
          await updateBoardPlace(boardId, place.boardPlaceId, nextTagIds)
          updated += 1
        } catch (error) {
          failures.push(`${place.name}: ${getApiErrorMessage(error)}`)
        }
      }

      return { total: placesById.size, updated, counts, failures }
    },
    onSuccess: ({ total, updated, counts, failures }) => {
      const summary = FALLBACK_TARGET_TAGS
        .map((name) => `${name} ${counts.get(name) ?? 0}`)
        .join(' · ')
      const failureSummary = failures.length > 0
        ? ` · 실패 ${failures.length}곳 (${failures.slice(0, 3).join(' / ')})`
        : ''
      setClassificationResult({
        message: `${total}곳 분류 완료 (${updated}곳 변경) · ${summary}${failureSummary}`,
        failed: failures.length > 0,
      })
      refresh()
    },
  })

  const handleCreate = (event: FormEvent) => {
    event.preventDefault()
    if (newTagName.trim()) createMutation.mutate()
  }

  const handleUpdate = (event: FormEvent) => {
    event.preventDefault()
    if (editing?.name.trim()) updateMutation.mutate({ ...editing, name: editing.name.trim() })
  }

  const error = createMutation.error || updateMutation.error || deleteMutation.error || classifyMutation.error

  return (
    <Modal title="태그 관리" onClose={onClose}>
      <form className="tag-row tag-create" onSubmit={handleCreate}>
        <IconPicker value={effectiveNewIcon} onChange={setNewTagIcon} />
        <input value={newTagName} onChange={(event) => setNewTagName(event.target.value)} placeholder="새 태그" maxLength={100} autoFocus />
        <button className="small" disabled={createMutation.isPending}>추가</button>
      </form>
      {error && <p className="error-banner">{getApiErrorMessage(error)}</p>}

      <div className="tag-row">
        <button
          type="button"
          className="secondary"
          disabled={classifyMutation.isPending}
          onClick={() => classifyMutation.mutate()}
        >
          {classifyMutation.isPending ? '추가 분류 중…' : '밥·기타 추가 분류'}
        </button>
        <span className="tag-name">밥·기타 태그 장소만 아시아·술·밥으로 정리</span>
      </div>
      {classificationResult && (
        <p className={classificationResult.failed ? 'error-banner' : 'success-banner'}>
          {classificationResult.message}
        </p>
      )}

      {tags.length === 0 ? (
        <p className="empty">등록된 태그가 없습니다.</p>
      ) : (
        <ul className="tag-manage-list">
          {tags.map((tag) => (
            <li key={tag.tagId}>
              {editing?.tagId === tag.tagId ? (
                <form className="tag-row" onSubmit={handleUpdate}>
                  <IconPicker value={editing.icon} onChange={(icon) => setEditing({ ...editing, icon })} />
                  <input
                    className="tag-name-input"
                    value={editing.name}
                    onChange={(event) => setEditing({ ...editing, name: event.target.value })}
                    maxLength={100}
                    autoFocus
                  />
                  <button className="small" disabled={updateMutation.isPending}>저장</button>
                  <button type="button" className="secondary small" onClick={() => setEditing(null)}>취소</button>
                </form>
              ) : (
                <div className="tag-row">
                  <span className="tag-icon" aria-hidden="true">{tag.icon || '#'}</span>
                  <span className="tag-name">{tag.name}</span>
                  <button type="button" className="secondary small" onClick={() => setEditing({ tagId: tag.tagId, name: tag.name, icon: tag.icon })}>
                    수정
                  </button>
                  <button
                    type="button"
                    className="danger small"
                    onClick={() => {
                      if (window.confirm(`#${tag.name} 태그를 삭제할까요? 장소에 붙은 태그도 함께 제거됩니다.`)) {
                        deleteMutation.mutate(tag.tagId)
                      }
                    }}
                  >
                    삭제
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </Modal>
  )
}

interface IconPickerProps {
  value: string | null
  onChange: (icon: string | null) => void
}

const PANEL_WIDTH = 248
const PANEL_HEIGHT = 190

function IconPicker({ value, onChange }: IconPickerProps) {
  // 모달 본문의 스크롤 영역에 잘리지 않도록 패널은 화면 기준(fixed)으로 띄운다.
  const [panelPosition, setPanelPosition] = useState<{ top: number; left: number } | null>(null)
  const open = panelPosition !== null
  const rootRef = useRef<HTMLDivElement>(null)
  const close = () => setPanelPosition(null)

  const toggle = () => {
    const rect = rootRef.current?.getBoundingClientRect()
    if (open || !rect) {
      close()
      return
    }
    const below = rect.bottom + PANEL_HEIGHT + 8 <= window.innerHeight
    setPanelPosition({
      top: Math.max(8, below ? rect.bottom + 4 : rect.top - PANEL_HEIGHT - 4),
      left: Math.max(8, Math.min(rect.left, window.innerWidth - PANEL_WIDTH - 8)),
    })
  }

  useEffect(() => {
    if (!open) return
    const closeOnOutside = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) close()
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation()
        close()
      }
    }
    // 위치를 고정해 두었으므로 스크롤·리사이즈 때는 닫는다.
    document.addEventListener('mousedown', closeOnOutside)
    document.addEventListener('keydown', closeOnEscape, true)
    window.addEventListener('resize', close)
    window.addEventListener('scroll', close, true)
    return () => {
      document.removeEventListener('mousedown', closeOnOutside)
      document.removeEventListener('keydown', closeOnEscape, true)
      window.removeEventListener('resize', close)
      window.removeEventListener('scroll', close, true)
    }
  }, [open])

  const choose = (icon: string | null) => {
    onChange(icon)
    close()
  }

  return (
    <div className="icon-picker" ref={rootRef}>
      <button
        type="button"
        className="tag-icon icon-picker-trigger"
        aria-label="태그 아이콘 선택"
        aria-expanded={open}
        onClick={toggle}
      >
        {value || '#'}
      </button>
      {open && (
        <div className="icon-picker-panel" style={panelPosition} role="listbox" aria-label="아이콘">
          <button type="button" className={value ? 'icon-choice none' : 'icon-choice none selected'} onClick={() => choose(null)}>
            없음
          </button>
          {TAG_ICON_CHOICES.map((icon) => (
            <button
              type="button"
              key={icon}
              role="option"
              aria-selected={icon === value}
              className={icon === value ? 'icon-choice selected' : 'icon-choice'}
              onClick={() => choose(icon)}
            >
              {icon}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
