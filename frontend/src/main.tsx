import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import { isClientError } from './api/client'
import { AuthGate } from './components/AuthGate'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    // 401·403·404 같은 요청 오류는 다시 시도해도 결과가 같으므로 재시도하지 않는다.
    queries: { retry: (failureCount, error) => !isClientError(error) && failureCount < 2 },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthGate>{(user, onLogout) => <App me={user} onLogout={onLogout} />}</AuthGate>
    </QueryClientProvider>
  </React.StrictMode>,
)
