import axios from 'axios'

// 로그인은 서버 세션 쿠키로 유지된다. 같은 origin(vite 프록시)이라 쿠키가 자동으로 붙는다.
export const apiClient = axios.create()

let unauthorizedHandler: (() => void) | null = null

// 세션이 만료되면 어느 요청에서든 401이 오므로, 로그인 화면으로 돌아가도록 한 곳에서 처리한다.
// 로그인 여부를 판단하는 /api/users/me 자체의 401은 제외한다(다시 me를 부르는 무한 반복 방지).
export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler
}

apiClient.interceptors.response.use(undefined, (error) => {
  if (axios.isAxiosError(error) && error.response?.status === 401 && !isAuthCheck(error.config?.url)) {
    unauthorizedHandler?.()
  }
  return Promise.reject(error)
})

function isAuthCheck(url: string | undefined): boolean {
  return url === '/api/users/me' || Boolean(url?.startsWith('/api/auth/'))
}

export function isClientError(error: unknown): boolean {
  const status = axios.isAxiosError(error) ? error.response?.status : undefined
  return status !== undefined && status >= 400 && status < 500
}

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? '서버에 연결할 수 없습니다.'
  }
  return '요청을 처리하지 못했습니다.'
}
