import { afterEach, describe, expect, it, vi } from 'vitest'
import worker from './index'

afterEach(() => vi.unstubAllGlobals())
const assets = { fetch: vi.fn(async () => new Response('<html>app</html>')) }
const baseEnv = { API_ORIGIN: 'http://127.0.0.1:5173', ASSETS: assets }

describe('private laptop API routing', () => {
  it('keeps static assets available during migration', async () => {
    const response = await worker.fetch(new Request('https://app.example/'), {
      ...baseEnv, MIGRATION_MAINTENANCE: 'true',
    })
    expect(await response.text()).toBe('<html>app</html>')
    expect(response.headers.get('Cross-Origin-Opener-Policy')).toBe('same-origin-allow-popups')
  })

  it('blocks API writes and reads before source data is copied', async () => {
    const upstream = vi.fn()
    for (const method of ['GET', 'POST', 'PATCH', 'DELETE']) {
      const response = await worker.fetch(new Request('https://app.example/api/boards', { method }), {
        ...baseEnv, MIGRATION_MAINTENANCE: 'true', LAPTOP_API: { fetch: upstream },
      })
      expect(response.status).toBe(503)
      expect(response.headers.get('Cache-Control')).toBe('no-store')
    }
    expect(upstream).not.toHaveBeenCalled()
  })

  it('allows only the health check through the private connection during migration', async () => {
    const upstream = vi.fn(async () => Response.json({status: 'UP'}))
    const response = await worker.fetch(new Request('https://app.example/api/health'), {
      ...baseEnv, MIGRATION_MAINTENANCE: 'true', LAPTOP_API: {fetch: upstream},
    })
    expect(response.status).toBe(200)
    expect(await response.json()).toEqual({status: 'UP'})
    expect(upstream).toHaveBeenCalledOnce()
  })

  it('preserves authenticated writes, query, cookies, and redirect status through VPC', async () => {
    const upstream = vi.fn(async (url, init) => {
      expect(url.toString()).toBe('http://127.0.0.1:5173/api/boards?view=all')
      expect(init.method).toBe('POST')
      expect(init.redirect).toBe('manual')
      expect(init.headers.get('cookie')).toBe('SESSION=example')
      expect(init.headers.get('forwarded')).toBeNull()
      expect(init.headers.get('X-Forwarded-Proto')).toBe('https')
      expect(init.headers.get('X-Forwarded-Host')).toBe('app.example')
      expect(await new Response(init.body).text()).toBe('{"name":"sample"}')
      return new Response(null, {status: 302, headers: {'Set-Cookie': 'SESSION=renewed; HttpOnly', Location: '/next'}})
    })
    const request = new Request('https://app.example/api/boards?view=all', {
      method: 'POST', headers: {Cookie: 'SESSION=example', Forwarded: 'proto=http;host=bad.example'}, body: '{"name":"sample"}',
    })
    const response = await worker.fetch(request, {...baseEnv, LAPTOP_API: {fetch: upstream}})
    expect(response.status).toBe(302)
    expect(response.headers.get('Set-Cookie')).toContain('SESSION=renewed')
    expect(response.headers.get('Location')).toBe('/next')
  })

  it('streams server-sent events without buffering', async () => {
    const stream = new ReadableStream({start(controller) {controller.enqueue(new TextEncoder().encode('data: changed\n\n')); controller.close()}})
    const response = await worker.fetch(new Request('https://app.example/api/boards/1/events'), {
      ...baseEnv, LAPTOP_API: {fetch: async () => new Response(stream, {headers: {'Content-Type': 'text/event-stream'}})},
    })
    expect(response.headers.get('Content-Type')).toBe('text/event-stream')
    expect(await response.text()).toBe('data: changed\n\n')
  })

  it('returns an offline response without sending writes to the original cloud DB', async () => {
    const publicFetch=vi.fn()
    vi.stubGlobal('fetch',publicFetch)
    const response=await worker.fetch(new Request('https://app.example/api/boards',{method:'POST'}),{
      ...baseEnv, LAPTOP_API:{fetch:async()=>{throw Error('Tunnel offline')}},
    })
    expect(response.status).toBe(503)
    expect(publicFetch).not.toHaveBeenCalled()
  })

  it('retains the original cloud route until the VPC binding is installed', async () => {
    const publicFetch=vi.fn(async()=>new Response('{"status":"UP"}',{headers:{'Content-Type':'application/json'}}))
    vi.stubGlobal('fetch',publicFetch)
    const response=await worker.fetch(new Request('https://app.example/api/health'),{...baseEnv,API_ORIGIN:'https://original.example'})
    expect(response.status).toBe(200)
    expect(publicFetch.mock.calls[0][0].toString()).toBe('https://original.example/api/health')
  })
})
