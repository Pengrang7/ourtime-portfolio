import { useState } from 'react'
import { Tag } from '../api/boardApi'
import { BoardPlace } from '../api/placeApi'
import { getApiErrorMessage } from '../api/client'
import { createSchedule } from '../api/scheduleApi'
import { Modal } from './Modal'
import { SchedulePlaceSearch } from './ScheduleModal'
import { ImportError, ImportedSchedule, parseScheduleText, toSchedulePayload } from './scheduleImport'

const EXAMPLE = `10월 7일 수요일, 성균관대역, 옷 챙겨서
10월 17일(토), 서초문화예술공원, 오전 10시부터 오후 6시
10월 30일~11월 1일, 경주`

interface Row extends ImportedSchedule {
  selected: boolean
  status: 'ready' | 'saved' | 'failed'
  saveError?: string
}

export function ScheduleImportModal({ boardId, places, tags, onClose, onSaved }: {
  boardId: number
  places: BoardPlace[]
  tags: Tag[]
  onClose: () => void
  onSaved: () => void
}) {
  const [text, setText] = useState('')
  const [rows, setRows] = useState<Row[] | null>(null)
  const [errors, setErrors] = useState<ImportError[]>([])
  const [saving, setSaving] = useState(false)

  const convert = () => {
    const result = parseScheduleText(text, places)
    setRows(result.schedules.map((item) => ({ ...item, selected: true, status: 'ready' })))
    setErrors(result.errors)
  }

  const update = (index: number, patch: Partial<Row>) => setRows((current) =>
    current?.map((row, rowIndex) => rowIndex === index ? { ...row, ...patch } : row) ?? null)

  const pending = rows?.filter((row) => row.selected && row.status !== 'saved') ?? []

  // 한 건이 실패해도 나머지는 저장하고, 실패한 줄만 남겨서 고친 뒤 다시 저장할 수 있게 한다.
  const save = async () => {
    if (!rows) return
    setSaving(true)
    const next = [...rows]
    for (let index = 0; index < next.length; index++) {
      const row = next[index]
      if (!row.selected || row.status === 'saved') continue
      try {
        await createSchedule(boardId, toSchedulePayload(row))
        next[index] = { ...row, status: 'saved', saveError: undefined }
      } catch (error) {
        next[index] = { ...row, status: 'failed', saveError: getApiErrorMessage(error) }
      }
      setRows([...next])
    }
    setSaving(false)
    onSaved()
    if (next.every((row) => !row.selected || row.status === 'saved')) onClose()
  }

  return (
    <Modal title="일정 한꺼번에 추가" onClose={onClose} panelClassName="schedule-modal schedule-import-modal">
      {rows === null ? (
        <div className="schedule-import-input">
          <p className="hint">한 줄에 일정 하나씩, <b>날짜, 장소, 메모</b> 순서로 적어 주세요. 시간이 없으면 종일 일정이 돼요.</p>
          <textarea rows={10} value={text} onChange={(event) => setText(event.target.value)} placeholder={EXAMPLE} autoFocus />
          <div className="button-row modal-actions">
            <button type="button" className="secondary" onClick={onClose}>닫기</button>
            <button type="button" disabled={!text.trim()} onClick={convert}>변환해서 확인하기</button>
          </div>
        </div>
      ) : (
        <div className="schedule-import-review">
          <p className="hint">저장하기 전에 확인하고 고칠 수 있어요. 보드에 있는 장소는 자동으로 연결했어요.</p>
          {errors.length > 0 && (
            <ul className="schedule-import-errors">
              {errors.map((item) => <li key={item.line}><b>{item.line}</b> — {item.error}</li>)}
            </ul>
          )}
          <ul className="schedule-import-rows">
            {rows.map((row, index) => (
              <li key={`${index}-${row.line}`} className={`schedule-import-row ${row.status}`}>
                <label className="schedule-import-check">
                  <input type="checkbox" checked={row.selected} disabled={row.status === 'saved' || saving}
                    onChange={(event) => update(index, { selected: event.target.checked })} />
                  <span>{row.status === 'saved' ? '저장됨' : row.line}</span>
                </label>
                <fieldset disabled={row.status === 'saved' || saving}>
                  <div className="schedule-import-dates">
                    <input type="date" aria-label="시작 날짜" value={row.startDate} onChange={(event) => update(index, {
                      startDate: event.target.value,
                      lastDate: event.target.value > row.lastDate ? event.target.value : row.lastDate,
                    })} />
                    <span>~</span>
                    <input type="date" aria-label="마지막 날짜" value={row.lastDate} min={row.startDate}
                      onChange={(event) => update(index, { lastDate: event.target.value })} />
                    <label className="schedule-all-day-toggle">
                      <input type="checkbox" checked={row.allDay} onChange={(event) => update(index, { allDay: event.target.checked })} />
                      종일
                    </label>
                    {!row.allDay && <>
                      <input type="time" aria-label="시작 시간" step={600} value={row.startTime} onChange={(event) => update(index, { startTime: event.target.value })} />
                      <span>~</span>
                      <input type="time" aria-label="종료 시간" step={600} value={row.endTime} onChange={(event) => update(index, { endTime: event.target.value })} />
                    </>}
                  </div>
                  <div className="schedule-import-fields">
                    <SchedulePlaceSearch places={places} tags={tags} value={row.boardPlaceId}
                      onChange={(boardPlaceId) => update(index, { boardPlaceId })} disabled={row.status === 'saved' || saving} />
                    <input aria-label="내용" placeholder="내용" maxLength={200} value={row.title} onChange={(event) => update(index, { title: event.target.value })} />
                    <input aria-label="메모" placeholder="메모" maxLength={2000} value={row.memo} onChange={(event) => update(index, { memo: event.target.value })} />
                  </div>
                </fieldset>
                {row.warnings.map((warning) => <p key={warning} className="notice">{warning}</p>)}
                {row.saveError && <p className="error-banner">{row.saveError}</p>}
              </li>
            ))}
          </ul>
          <div className="button-row modal-actions">
            <button type="button" className="secondary" disabled={saving} onClick={() => setRows(null)}>다시 붙여넣기</button>
            <button type="button" disabled={saving || pending.length === 0} onClick={save}>
              {saving ? '저장 중' : `${pending.length}개 저장`}
            </button>
          </div>
        </div>
      )}
    </Modal>
  )
}
