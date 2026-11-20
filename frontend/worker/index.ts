interface Env {
  API_ORIGIN: string
  ASSETS: Fetcher
  LAPTOP_API?: Fetcher
  MIGRATION_MAINTENANCE?: string
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const requestUrl = new URL(request.url)
    if (!requestUrl.pathname.startsWith('/api/')) {
      const response = await env.ASSETS.fetch(request)
      const headers = new Headers(response.headers)
      headers.set('Cross-Origin-Opener-Policy', 'same-origin-allow-popups')
      return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers,
      })
    }

    if (env.MIGRATION_MAINTENANCE === 'true' && requestUrl.pathname !== '/api/health') {
      return Response.json(
        { code: 'MAINTENANCE', message: '데이터 이전 중입니다. 잠시 후 다시 시도해 주세요.' },
        { status: 503, headers: { 'Retry-After': '60', 'Cache-Control': 'no-store' } },
      )
    }

    const apiOrigin = env.API_ORIGIN?.trim().replace(/\/+$/, '')
    if (!apiOrigin) {
      return new Response('API_ORIGIN is not configured', { status: 500 })
    }

    let upstreamUrl: URL
    try {
      upstreamUrl = new URL(`${requestUrl.pathname}${requestUrl.search}`, `${apiOrigin}/`)
    } catch {
      return new Response('API_ORIGIN is invalid', { status: 500 })
    }

    const headers = new Headers(request.headers)
    headers.delete('host')
    headers.delete('forwarded')
    headers.set('X-Forwarded-Proto', requestUrl.protocol.slice(0, -1))
    headers.set('X-Forwarded-Host', requestUrl.host)

    const method = request.method.toUpperCase()
    const init: RequestInit = {
      method,
      headers,
      redirect: 'manual',
    }

    if (method !== 'GET' && method !== 'HEAD') {
      init.body = request.body
    }

    let response: Response
    try {
      // A configured private origin must never fall back to the old database.
      response = env.LAPTOP_API
        ? await env.LAPTOP_API.fetch(upstreamUrl, init)
        : await fetch(upstreamUrl, init)
    } catch {
      return Response.json(
        { code: 'SERVICE_UNAVAILABLE', message: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.' },
        { status: 503, headers: { 'Retry-After': '30', 'Cache-Control': 'no-store' } },
      )
    }
    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
    })
  },
}
