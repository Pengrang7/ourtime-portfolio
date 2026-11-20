import { apiClient } from './client'

export interface Board {
  boardId: number
  name: string
  mapType: BoardMapType
  ownerId: number
  ownerNickname: string
}

export type BoardMapType = 'DOMESTIC' | 'OVERSEAS'

export interface BoardMember {
  userId: number
  email: string
  // 초대만 받고 아직 접속하지 않은 사람은 닉네임이 없다.
  nickname: string | null
  owner: boolean
  canEditPlace: boolean
  canManageTag: boolean
  canDeletePlace: boolean
  canInviteMember: boolean
}

export interface Tag {
  tagId: number
  boardId: number
  name: string
  icon: string | null
}

export async function fetchBoards(): Promise<Board[]> {
  return (await apiClient.get<Board[]>('/api/boards')).data
}

export async function createBoard(name: string, mapType: BoardMapType): Promise<Board> {
  return (await apiClient.post<Board>('/api/boards', { name, mapType })).data
}

export async function renameBoard(boardId: number, name: string): Promise<Board> {
  return (await apiClient.patch<Board>(`/api/boards/${boardId}`, { name })).data
}

export async function fetchMembers(boardId: number): Promise<BoardMember[]> {
  return (await apiClient.get<BoardMember[]>(`/api/boards/${boardId}/members`)).data
}

export interface MemberPermissions {
  canEditPlace: boolean
  canManageTag: boolean
  canDeletePlace: boolean
  canInviteMember: boolean
}

export async function inviteMember(
  boardId: number,
  email: string,
  permissions: MemberPermissions,
): Promise<BoardMember> {
  return (await apiClient.post<BoardMember>(`/api/boards/${boardId}/members`, {
    email,
    ...permissions,
  })).data
}

export async function updateMemberPermissions(
  boardId: number,
  userId: number,
  permissions: MemberPermissions,
): Promise<BoardMember> {
  return (await apiClient.patch<BoardMember>(`/api/boards/${boardId}/members/${userId}`, permissions)).data
}

export async function fetchTags(boardId: number): Promise<Tag[]> {
  return (await apiClient.get<Tag[]>(`/api/boards/${boardId}/tags`)).data
}

export async function createTag(boardId: number, name: string, icon: string | null): Promise<Tag> {
  return (await apiClient.post<Tag>(`/api/boards/${boardId}/tags`, { name, icon })).data
}

export async function updateTag(boardId: number, tagId: number, name: string, icon: string | null): Promise<void> {
  await apiClient.patch(`/api/boards/${boardId}/tags/${tagId}`, { name, icon })
}

export async function deleteTag(boardId: number, tagId: number): Promise<void> {
  await apiClient.delete(`/api/boards/${boardId}/tags/${tagId}`)
}
