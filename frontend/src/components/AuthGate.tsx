import { FormEvent, ReactNode, useEffect, useRef, useState } from 'react'
import { QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CurrentUser, fetchAuthConfig, fetchMe, loginWithGoogle, logout, updateMyNickname } from '../api/authApi'
import { getApiErrorMessage, isClientError, setUnauthorizedHandler } from '../api/client'
import type { GoogleGlobal } from '../googleTypes'

const GOOGLE_SCRIPT_ID = 'google-identity-script'
const SUGGESTED_NICKNAME_KEY = 'placeBoard.suggestedNickname'

declare global {
  interface Window {
    google?: GoogleGlobal
  }
}

// 사용자가 바뀔 때 이전 사용자의 보드 데이터가 남지 않도록 로그인 설정 외의 캐시를 비운다.
function dropUserData(queryClient: QueryClient) {
  queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'authConfig' && query.queryKey[0] !== 'me' })
}

let googleScriptPromise: Promise<void> | null = null

function loadGoogleScript(): Promise<void> {
  if (window.google?.accounts) return Promise.resolve()
  if (googleScriptPromise) return googleScriptPromise
  googleScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.id = GOOGLE_SCRIPT_ID
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => {
      googleScriptPromise = null
      reject(new Error('구글 로그인 스크립트를 불러오지 못했습니다.'))
    }
    document.head.appendChild(script)
  })
  return googleScriptPromise
}

interface AuthGateProps {
  children: (user: CurrentUser & { nickname: string }, onLogout: () => void) => ReactNode
}

// 로그인 → (처음이면) 이름 입력 → 앱 순서로 보여준다.
export function AuthGate({ children }: AuthGateProps) {
  const queryClient = useQueryClient()
  const configQuery = useQuery({ queryKey: ['authConfig'], queryFn: fetchAuthConfig, staleTime: Infinity })
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
    enabled: configQuery.isSuccess,
    retry: false,
  })

  useEffect(() => {
    setUnauthorizedHandler(() => queryClient.invalidateQueries({ queryKey: ['me'] }))
  }, [queryClient])

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      window.google?.accounts?.id.disableAutoSelect()
      dropUserData(queryClient)
      queryClient.resetQueries({ queryKey: ['me'] })
    },
  })

  if (configQuery.isPending || (configQuery.isSuccess && meQuery.isPending)) return <div className="auth-screen" />

  if (configQuery.isError) {
    return (
      <AuthCard>
        <p className="error-banner">{getApiErrorMessage(configQuery.error)}</p>
        <button type="button" onClick={() => configQuery.refetch()}>다시 시도</button>
      </AuthCard>
    )
  }

  if (meQuery.isError) {
    if (!isClientError(meQuery.error)) {
      return (
        <AuthCard>
          <p className="error-banner">{getApiErrorMessage(meQuery.error)}</p>
          <button type="button" onClick={() => meQuery.refetch()}>다시 시도</button>
        </AuthCard>
      )
    }
    return <LoginScreen clientId={configQuery.data?.googleClientId ?? null} />
  }

  if (!meQuery.data) return <div className="auth-screen" />

  const user = meQuery.data
  if (!user.nickname) return <NicknameSetup user={user} onLogout={() => logoutMutation.mutate()} />
  return <>{children({ ...user, nickname: user.nickname }, () => logoutMutation.mutate())}</>
}

function AuthCard({ children }: { children: ReactNode }) {
  return (
    <div className="auth-screen">
      <section className="auth-card">
        <div className="auth-heart" aria-hidden="true" />
        <h1 className="auth-brand">우리의 장소</h1>
        <p className="auth-lead">함께 가고 싶은 곳과 소중한 추억을 한곳에</p>
        {children}
      </section>
    </div>
  )
}

function LoginScreen({ clientId }: { clientId: string | null }) {
  const queryClient = useQueryClient()
  const buttonRef = useRef<HTMLDivElement>(null)
  const [scriptError, setScriptError] = useState<string | null>(null)

  const loginMutation = useMutation({
    mutationFn: loginWithGoogle,
    onSuccess: (result) => {
      try {
        if (result.suggestedNickname) sessionStorage.setItem(SUGGESTED_NICKNAME_KEY, result.suggestedNickname)
      } catch {
        // 저장소를 못 쓰면 이름 칸을 비워 둘 뿐이다.
      }
      dropUserData(queryClient)
      queryClient.setQueryData(['me'], result.user)
    },
  })
  const loginRef = useRef(loginMutation.mutate)
  loginRef.current = loginMutation.mutate

  useEffect(() => {
    if (!clientId) return
    let cancelled = false
    loadGoogleScript()
      .then(() => {
        const accounts = window.google?.accounts
        if (cancelled || !accounts || !buttonRef.current) return
        accounts.id.initialize({ client_id: clientId, callback: (response) => loginRef.current(response.credential) })
        accounts.id.renderButton(buttonRef.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'signin_with',
          shape: 'pill',
          locale: 'ko',
          width: 260,
        })
      })
      .catch((error: Error) => {
        if (!cancelled) setScriptError(error.message)
      })
    return () => {
      cancelled = true
    }
  }, [clientId])

  return (
    <AuthCard>
      {clientId ? (
        <div className="google-button" ref={buttonRef} />
      ) : (
        <p className="notice">
          구글 로그인이 아직 설정되지 않았습니다.
          <br />
          백엔드 실행 환경에 <code>GOOGLE_CLIENT_ID</code>를 넣고 재시작해 주세요.
        </p>
      )}
      {loginMutation.isPending && <p className="hint">로그인 중입니다…</p>}
      {(scriptError || loginMutation.error) && (
        <p className="error-banner">{scriptError ?? getApiErrorMessage(loginMutation.error)}</p>
      )}
    </AuthCard>
  )
}

function NicknameSetup({ user, onLogout }: { user: CurrentUser; onLogout: () => void }) {
  const queryClient = useQueryClient()
  const [nickname, setNickname] = useState(() => {
    try {
      return sessionStorage.getItem(SUGGESTED_NICKNAME_KEY) ?? ''
    } catch {
      return ''
    }
  })
  const saveMutation = useMutation({
    mutationFn: () => updateMyNickname(nickname.trim()),
    onSuccess: (updated) => {
      try {
        sessionStorage.removeItem(SUGGESTED_NICKNAME_KEY)
      } catch {
        // 무시
      }
      queryClient.setQueryData(['me'], updated)
    },
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (nickname.trim()) saveMutation.mutate()
  }

  return (
    <AuthCard>
      <form className="stack-form nickname-form" onSubmit={handleSubmit}>
        <label htmlFor="nickname-input">보드 멤버에게 보일 이름을 정해 주세요.</label>
        <input id="nickname-input" value={nickname} onChange={(event) => setNickname(event.target.value)} maxLength={100} autoFocus required />
        <button disabled={saveMutation.isPending || !nickname.trim()}>시작하기</button>
      </form>
      {saveMutation.error && <p className="error-banner">{getApiErrorMessage(saveMutation.error)}</p>}
      <p className="hint">
        {user.email} ·{' '}
        <button type="button" className="link-button" onClick={onLogout}>다른 계정으로 로그인</button>
      </p>
    </AuthCard>
  )
}
