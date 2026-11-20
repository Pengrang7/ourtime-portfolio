import { FormEvent, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { CyclePeriod, CyclePeriodInput, createCyclePeriod, deleteCyclePeriod, updateCyclePeriod } from '../api/cycleApi'
import { getApiErrorMessage } from '../api/client'
import { addDays, dateKey } from './scheduleCalendar'
import { formatDateRange, statsFor } from './cycleCalendar'
import { Modal } from './Modal'

interface CyclePeriodModalProps {
  periods: CyclePeriod[]
  onClose: () => void
  onChanged: (period?: CyclePeriod) => void
}

const emptyInput = (): CyclePeriodInput => ({ startDate: dateKey(new Date()), endDate: dateKey(new Date()) })
const maxEndDate = (startDate: string): string => dateKey(addDays(new Date(`${startDate}T00:00:00`), 13))

export function CyclePeriodModal({ periods, onClose, onChanged }: CyclePeriodModalProps) {
  const [draft, setDraft] = useState<CyclePeriodInput>(emptyInput)
  const [editingId, setEditingId] = useState<number | null>(null)
  const stats = statsFor(periods)
  const saveMutation = useMutation({
    mutationFn: ({ periodId, input }: { periodId: number | null; input: CyclePeriodInput }) =>
      periodId === null ? createCyclePeriod(input) : updateCyclePeriod(periodId, input),
    onSuccess: (period) => {
      setDraft(emptyInput())
      setEditingId(null)
      onChanged(period)
    },
  })
  const deleteMutation = useMutation({ mutationFn: deleteCyclePeriod, onSuccess: () => onChanged() })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    saveMutation.mutate({ periodId: editingId, input: { ...draft } })
  }
  const error = saveMutation.error || deleteMutation.error

  return (
    <Modal title="생리 기록" onClose={onClose}>
      <form className="cycle-form" onSubmit={submit}>
        <div className="cycle-number-fields">
          <label className="field">시작일
            <input type="date" required max={dateKey(new Date())} value={draft.startDate} onChange={(event) => {
              const startDate = event.target.value
              setDraft({ ...draft, startDate, endDate: editingId === null || draft.endDate < startDate ? startDate : draft.endDate })
            }} />
          </label>
          <label className="field">종료일
            <input type="date" required min={draft.startDate} max={maxEndDate(draft.startDate)} value={draft.endDate} onChange={(event) => setDraft({ ...draft, endDate: event.target.value })} />
          </label>
        </div>
        {error && <p className="error-banner">{getApiErrorMessage(error)}</p>}
        <div className="button-row modal-actions">
          {editingId !== null && <button type="button" className="secondary" onClick={() => { setEditingId(null); setDraft(emptyInput()) }}>취소</button>}
          <button disabled={saveMutation.isPending}>{editingId === null ? '기록 추가' : '수정 저장'}</button>
        </div>
      </form>

      {stats && (
        <div className="cycle-preview">
          <p><i className="cycle-dot period" />평균 생리 기간 <strong>{stats.averagePeriodLength.toFixed(1)}일</strong></p>
          <p><i className="cycle-dot fertile" />평균 생리 주기 <strong>{stats.averageCycleLength === null ? '2회 이상 기록 필요' : `${stats.averageCycleLength.toFixed(1)}일 (시작일 기준)`}</strong></p>
          {stats.nextPeriodStart && stats.nextPeriodEnd && <p><i className="cycle-dot period" />다음 생리 예정 <strong>{formatDateRange(stats.nextPeriodStart, stats.nextPeriodEnd)}</strong></p>}
          {stats.fertileBeforeStart && stats.fertileBeforeEnd && stats.fertileAfterStart && stats.fertileAfterEnd && (
            <p><i className="cycle-dot fertile" />가임기 <strong>{formatDateRange(stats.fertileBeforeStart, stats.fertileBeforeEnd)}, {formatDateRange(stats.fertileAfterStart, stats.fertileAfterEnd)}</strong></p>
          )}
          {stats.ovulationStart && stats.ovulationEnd && <p><i className="cycle-dot ovulation" />배란 예정일 <strong>{formatDateRange(stats.ovulationStart, stats.ovulationEnd)}</strong></p>}
        </div>
      )}

      <div className="cycle-history">
        <h4>기록 내역</h4>
        {periods.length === 0 ? <p className="empty">아직 생리 기록이 없습니다.</p> : (
          <ul>
            {periods.map((period) => {
              const duration = Math.round((new Date(`${period.endDate}T00:00:00`).getTime() - new Date(`${period.startDate}T00:00:00`).getTime()) / 86_400_000) + 1
              return <li key={period.periodId}>
                <span><strong>{period.startDate}</strong> ~ {period.endDate} · {duration}일</span>
                <button type="button" className="secondary small" onClick={() => { setEditingId(period.periodId); setDraft({ startDate: period.startDate, endDate: period.endDate }) }}>수정</button>
                <button type="button" className="danger small" onClick={() => {
                  if (window.confirm('이 생리 기록을 삭제할까요?')) deleteMutation.mutate(period.periodId)
                }}>삭제</button>
              </li>
            })}
          </ul>
        )}
      </div>
      <p className="cycle-privacy">기록과 계산 결과는 함께 사용하는 보드의 구성원에게 표시됩니다. 예측값은 피임이나 의료 판단에 사용할 수 없습니다.</p>
    </Modal>
  )
}
