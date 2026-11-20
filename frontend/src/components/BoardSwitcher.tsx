import { FormEvent, useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Board, BoardMapType, createBoard, renameBoard } from '../api/boardApi'
import { getApiErrorMessage } from '../api/client'

interface BoardSwitcherProps {
  boards: Board[]
  currentBoard: Board | undefined
  canRename: boolean
  onSelect: (boardId: number) => void
}

// 보드 이름을 누르면 보드 목록·이름 수정·새 보드 만들기가 한 드롭다운에 열린다.
export function BoardSwitcher({ boards, currentBoard, canRename, onSelect }: BoardSwitcherProps) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [renaming, setRenaming] = useState<string | null>(null)
  const [newBoardName, setNewBoardName] = useState('')
  const [newBoardMapType, setNewBoardMapType] = useState<BoardMapType>('DOMESTIC')
  const rootRef = useRef<HTMLDivElement>(null)

  const close = () => {
    setOpen(false)
    setRenaming(null)
  }

  useEffect(() => {
    if (!open) return
    const closeOnOutside = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) close()
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') close()
    }
    document.addEventListener('mousedown', closeOnOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])

  const renameMutation = useMutation({
    mutationFn: (name: string) => renameBoard(currentBoard!.boardId, name),
    onSuccess: () => {
      setRenaming(null)
      queryClient.invalidateQueries({ queryKey: ['boards'] })
    },
  })
  const createMutation = useMutation({
    mutationFn: ({ name, mapType }: { name: string; mapType: BoardMapType }) => createBoard(name, mapType),
    onSuccess: (board) => {
      setNewBoardName('')
      close()
      onSelect(board.boardId)
      queryClient.invalidateQueries({ queryKey: ['boards'] })
    },
  })

  const handleRename = (event: FormEvent) => {
    event.preventDefault()
    if (renaming?.trim()) renameMutation.mutate(renaming.trim())
  }
  const handleCreate = (event: FormEvent) => {
    event.preventDefault()
    if (newBoardName.trim()) createMutation.mutate({ name: newBoardName.trim(), mapType: newBoardMapType })
  }

  const error = renameMutation.error || createMutation.error

  return (
    <div className="board-switcher" ref={rootRef}>
      <button
        type="button"
        className="board-switcher-trigger"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => (open ? close() : setOpen(true))}
      >
        <span className="board-switcher-name">{currentBoard?.name ?? '보드 선택'}</span>
        <span aria-hidden="true">▾</span>
      </button>

      {open && (
        <div className="board-menu">
          {boards.length > 0 && (
            <ul className="board-menu-list" aria-label="보드 목록">
              {boards.map((board) => (
                <li key={board.boardId}>
                  <button
                    type="button"
                    className={board.boardId === currentBoard?.boardId ? 'board-menu-item active' : 'board-menu-item'}
                    onClick={() => {
                      onSelect(board.boardId)
                      close()
                    }}
                  >
                    <span className="board-menu-check" aria-hidden="true">{board.boardId === currentBoard?.boardId ? '✓' : ''}</span>
                    <span className="board-menu-label">{board.mapType !== 'OVERSEAS' ? '🇰🇷' : '🌏'} {board.name}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          {currentBoard && canRename && (
            renaming === null ? (
              <button type="button" className="board-menu-item" onClick={() => setRenaming(currentBoard.name)}>
                <span className="board-menu-check" aria-hidden="true">✏️</span>
                <span className="board-menu-label">현재 보드 이름 수정</span>
              </button>
            ) : (
              <form className="board-menu-form" onSubmit={handleRename}>
                <input
                  value={renaming}
                  onChange={(event) => setRenaming(event.target.value)}
                  maxLength={150}
                  aria-label="보드 이름"
                  autoFocus
                />
                <button className="small" disabled={renameMutation.isPending}>저장</button>
                <button type="button" className="secondary small" onClick={() => setRenaming(null)}>취소</button>
              </form>
            )
          )}

          <form className="board-menu-form" onSubmit={handleCreate}>
            <input
              value={newBoardName}
              onChange={(event) => setNewBoardName(event.target.value)}
              placeholder="새 보드 이름"
              maxLength={150}
              aria-label="새 보드 이름"
            />
            <select value={newBoardMapType} onChange={(event) => setNewBoardMapType(event.target.value as BoardMapType)} aria-label="지도 유형">
              <option value="DOMESTIC">국내</option>
              <option value="OVERSEAS">해외</option>
            </select>
            <button className="small" disabled={createMutation.isPending}>생성</button>
          </form>

          {error && <p className="error-banner">{getApiErrorMessage(error)}</p>}
        </div>
      )}
    </div>
  )
}
