import { FormEvent, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Tag } from '../api/boardApi'
import { getApiErrorMessage } from '../api/client'
import { BoardPlace, PlacePreview, previewPlace, savePlace } from '../api/placeApi'
import { Modal } from './Modal'
import { tagLabel } from './placeIcons'

const PROVIDER_LABELS: Record<string, string> = {
  NAVER: '네이버',
  KAKAO: '카카오',
  GOOGLE: '구글',
}

interface AddPlaceModalProps {
  boardId: number
  tags: Tag[]
  onClose: () => void
  onSaved: (place: BoardPlace) => void
}

export function AddPlaceModal({ boardId, tags, onClose, onSaved }: AddPlaceModalProps) {
  const [url, setUrl] = useState('')
  const [preview, setPreview] = useState<PlacePreview | null>(null)
  const [tagIds, setTagIds] = useState<number[]>([])

  const previewMutation = useMutation({
    mutationFn: previewPlace,
    onMutate: () => setPreview(null),
    onSuccess: setPreview,
  })
  const saveMutation = useMutation({
    mutationFn: () => savePlace(boardId, { ...preview!, tagIds }),
    onSuccess: onSaved,
  })

  const handlePreview = (event: FormEvent) => {
    event.preventDefault()
    if (url.trim()) previewMutation.mutate(url.trim())
  }

  const toggleTag = (tagId: number) => setTagIds((current) =>
    current.includes(tagId) ? current.filter((id) => id !== tagId) : [...current, tagId])

  const error = previewMutation.error || saveMutation.error

  return (
    <Modal title="장소 추가" onClose={onClose}>
      <form className="compact-form" onSubmit={handlePreview}>
        <input
          type="url"
          value={url}
          onChange={(event) => setUrl(event.target.value)}
          placeholder="네이버·카카오·구글 지도 장소 링크"
          autoFocus
          required
        />
        <button disabled={previewMutation.isPending}>{previewMutation.isPending ? '불러오는 중' : '불러오기'}</button>
      </form>
      <p className="hint">예: https://naver.me/…, https://kko.kakao.com/…, https://maps.app.goo.gl/…</p>

      {error && <p className="error-banner">{getApiErrorMessage(error)}</p>}

      {preview && (
        <form className="add-place-form" onSubmit={(event) => {
          event.preventDefault()
          saveMutation.mutate()
        }}>
          <div className="preview-card">
            <div className="preview-title">
              <span className={`provider provider-${preview.provider.toLowerCase()}`}>
                {PROVIDER_LABELS[preview.provider] ?? preview.provider}
              </span>
              <strong>{preview.name}</strong>
            </div>
            {preview.category && <p className="preview-category">{preview.category}</p>}
            <p>{preview.address}</p>
            {preview.phone && <p>{preview.phone}</p>}
            {preview.latitude === null && <p className="notice">좌표를 찾지 못해 지도에는 표시되지 않습니다.</p>}
          </div>

          <fieldset className="field">
            <legend>태그</legend>
            {tags.length === 0 ? (
              <p className="hint">등록된 태그가 없습니다. 태그 관리에서 먼저 추가해 주세요.</p>
            ) : (
              <div className="tag-actions">
                {tags.map((tag) => (
                  <button
                    type="button"
                    key={tag.tagId}
                    className={tagIds.includes(tag.tagId) ? 'tag-button selected' : 'tag-button'}
                    onClick={() => toggleTag(tag.tagId)}
                  >
                    {tagLabel(tag.name, tag.icon)}
                  </button>
                ))}
              </div>
            )}
          </fieldset>

          <div className="button-row modal-actions">
            <button type="button" className="secondary" onClick={onClose}>취소</button>
            <button disabled={saveMutation.isPending}>{saveMutation.isPending ? '저장 중' : '저장'}</button>
          </div>
        </form>
      )}
    </Modal>
  )
}
