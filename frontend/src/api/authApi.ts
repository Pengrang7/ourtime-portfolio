import { apiClient } from './client'

export interface CurrentUser {
  userId: number
  email: string
  // 초대받고 처음 로그인했거나 새로 가입한 사람은 아직 이름이 없다.
  nickname: string | null
}

export interface AuthConfig {
  googleClientId: string | null
  headerLoginEnabled: boolean
}

export interface LoginResult {
  user: CurrentUser
  suggestedNickname: string | null
}

export async function fetchAuthConfig(): Promise<AuthConfig> {
  return (await apiClient.get<AuthConfig>('/api/auth/config')).data
}

export async function fetchMe(): Promise<CurrentUser> {
  return (await apiClient.get<CurrentUser>('/api/users/me')).data
}

export async function loginWithGoogle(credential: string): Promise<LoginResult> {
  return (await apiClient.post<LoginResult>('/api/auth/google', { credential })).data
}

export async function logout(): Promise<void> {
  await apiClient.post('/api/auth/logout')
}

export async function updateMyNickname(nickname: string): Promise<CurrentUser> {
  return (await apiClient.patch<CurrentUser>('/api/users/me', { nickname })).data
}
