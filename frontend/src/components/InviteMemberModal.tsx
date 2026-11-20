import { FormEvent, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { BoardMember, MemberPermissions, inviteMember, updateMemberPermissions } from '../api/boardApi'
import { getApiErrorMessage } from '../api/client'
import { Modal } from './Modal'

const PERMISSION_LABELS: [keyof MemberPermissions, string][] = [
  ['canEditPlace', '장소 원본 수정'],
  ['canManageTag', '태그 관리'],
  ['canDeletePlace', '장소 삭제'],
  ['canInviteMember', '멤버 초대'],
]

const EMPTY_PERMISSIONS: MemberPermissions = {
  canEditPlace: false,
  canManageTag: false,
  canDeletePlace: false,
  canInviteMember: false,
}

interface InviteMemberModalProps {
  boardId: number
  members: BoardMember[]
  currentUserId: number
  canManageMembers: boolean
  onClose: () => void
}

function permissionsOf(member: BoardMember): MemberPermissions {
  return {
    canEditPlace: member.canEditPlace,
    canManageTag: member.canManageTag,
    canDeletePlace: member.canDeletePlace,
    canInviteMember: member.canInviteMember,
  }
}

export function InviteMemberModal({ boardId, members, currentUserId, canManageMembers, onClose }: InviteMemberModalProps) {
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [permissions, setPermissions] = useState(EMPTY_PERMISSIONS)
  const [editingUserId, setEditingUserId] = useState<number | null>(null)
  const [editingPermissions, setEditingPermissions] = useState(EMPTY_PERMISSIONS)
  const editingMember = members.find((member) => member.userId === editingUserId)

  const inviteMutation = useMutation({
    mutationFn: () => inviteMember(boardId, email, permissions),
    onSuccess: () => {
      setEmail('')
      setPermissions(EMPTY_PERMISSIONS)
      queryClient.invalidateQueries({ queryKey: ['members', boardId] })
    },
  })
  const permissionMutation = useMutation({
    mutationFn: () => updateMemberPermissions(boardId, editingUserId!, editingPermissions),
    onSuccess: () => {
      setEditingUserId(null)
      queryClient.invalidateQueries({ queryKey: ['members', boardId] })
    },
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    inviteMutation.mutate()
  }

  return (
    <Modal title="멤버 관리" onClose={onClose}>
      {canManageMembers && <form className="stack-form" onSubmit={handleSubmit}>
        <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="친구 이메일" autoFocus required />
        <p className="hint">이름은 초대받은 사람이 처음 접속할 때 직접 입력합니다.</p>
        <div className="permission-grid">
          {PERMISSION_LABELS.map(([key, label]) => (
            <label key={key}>
              <input
                type="checkbox"
                checked={permissions[key]}
                onChange={(event) => setPermissions((current) => ({ ...current, [key]: event.target.checked }))}
              />
              {label}
            </label>
          ))}
        </div>
        <button disabled={inviteMutation.isPending}>초대</button>
      </form>}
      {inviteMutation.error && <p className="error-banner">{getApiErrorMessage(inviteMutation.error)}</p>}
      {permissionMutation.error && <p className="error-banner">{getApiErrorMessage(permissionMutation.error)}</p>}
      {inviteMutation.isSuccess && <p className="notice">초대했습니다.</p>}

      <h4 className="modal-subtitle">멤버 {members.length}명</h4>
      <ul className="member-list">
        {members.map((member) => (
          <li key={member.userId}>
            <strong>{member.nickname ?? member.email}</strong>
            <span>{member.nickname ? member.email : ''}</span>
            {member.owner && <span className="chip">소유자</span>}
            {!member.nickname && <span className="chip pending">가입 대기</span>}
            {canManageMembers && !member.owner && member.userId !== currentUserId && (
              <button type="button" className="secondary small" onClick={() => {
                setEditingUserId(member.userId)
                setEditingPermissions(permissionsOf(member))
              }}>권한 수정</button>
            )}
          </li>
        ))}
      </ul>
      {editingUserId !== null && (
        <form className="stack-form" onSubmit={(event) => {
          event.preventDefault()
          permissionMutation.mutate()
        }}>
          <h4 className="modal-subtitle">
            {editingMember?.nickname ?? editingMember?.email} 권한 수정
          </h4>
          <div className="permission-grid">
            {PERMISSION_LABELS.map(([key, label]) => (
              <label key={key}>
                <input
                  type="checkbox"
                  checked={editingPermissions[key]}
                  onChange={(event) => setEditingPermissions((current) => ({ ...current, [key]: event.target.checked }))}
                />
                {label}
              </label>
            ))}
          </div>
          <div className="button-row">
            <button disabled={permissionMutation.isPending}>권한 저장</button>
            <button type="button" className="secondary" onClick={() => setEditingUserId(null)}>취소</button>
          </div>
        </form>
      )}
    </Modal>
  )
}
