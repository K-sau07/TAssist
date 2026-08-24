import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { apiFetch, ApiError } from '../client'
import { useAuthStore } from '@/lib/auth/store'

// Helper to stub global fetch with a controlled Response.
function mockFetch(status: number, body: unknown, contentType = 'application/json') {
  const res = {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'content-type': contentType }),
    json: async () => body,
    text: async () => (typeof body === 'string' ? body : JSON.stringify(body)),
  }
  return vi.fn().mockResolvedValue(res as unknown as Response)
}

describe('apiFetch', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null } as never)
  })
  afterEach(() => vi.restoreAllMocks())

  it('returns parsed JSON on success', async () => {
    vi.stubGlobal('fetch', mockFetch(200, { hello: 'world' }))
    const data = await apiFetch<{ hello: string }>('/thing', { auth: false })
    expect(data).toEqual({ hello: 'world' })
  })

  it('returns undefined on 204 No Content', async () => {
    vi.stubGlobal('fetch', mockFetch(204, ''))
    const data = await apiFetch('/thing', { auth: false })
    expect(data).toBeUndefined()
  })

  it('parses the NESTED §17.4 error envelope { error: { code, message } }', async () => {
    // This is the exact shape that caused the signup-422 bug — must stay covered.
    vi.stubGlobal('fetch', mockFetch(422, {
      error: { code: 'VALIDATION_ERROR', message: 'Password too short', details: { password: 'min 10' } },
    }))
    await expect(apiFetch('/auth/signup', { auth: false })).rejects.toMatchObject({
      status: 422,
      code: 'VALIDATION_ERROR',
      message: 'Password too short',
      details: { password: 'min 10' },
    })
  })

  it('falls back to a flat error body when not nested', async () => {
    vi.stubGlobal('fetch', mockFetch(400, { code: 'BAD', message: 'flat error' }))
    await expect(apiFetch('/thing', { auth: false })).rejects.toMatchObject({
      status: 400, code: 'BAD', message: 'flat error',
    })
  })

  it('extracts retryAfterSeconds from details for RATE_LIMITED', async () => {
    vi.stubGlobal('fetch', mockFetch(429, {
      error: { code: 'RATE_LIMITED', message: 'slow down', details: { retryAfterSeconds: '30' } },
    }))
    try {
      await apiFetch('/thing', { auth: false })
      expect.unreachable('should have thrown')
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError)
      expect((e as ApiError).retryAfterSeconds).toBe(30)
    }
  })

  it('survives a non-JSON error body without crashing', async () => {
    const res = {
      ok: false, status: 500, headers: new Headers({ 'content-type': 'text/html' }),
      json: async () => { throw new Error('not json') },
      text: async () => '<html>500</html>',
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(res as unknown as Response))
    await expect(apiFetch('/thing', { auth: false })).rejects.toMatchObject({ status: 500 })
  })

  it('attaches the Bearer token when authenticated', async () => {
    useAuthStore.setState({ token: 'tok-123', user: null } as never)
    const spy = mockFetch(200, {})
    vi.stubGlobal('fetch', spy)
    await apiFetch('/secure')
    const headers = (spy.mock.calls[0][1] as RequestInit).headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer tok-123')
  })

  it('clears auth on 401', async () => {
    useAuthStore.setState({ token: 'expired', user: { id: 'x' } } as never)
    vi.stubGlobal('fetch', mockFetch(401, { error: { message: 'expired' } }))
    await expect(apiFetch('/secure')).rejects.toBeInstanceOf(ApiError)
    expect(useAuthStore.getState().token).toBeNull()
  })

  it('sends JSON body with correct content-type', async () => {
    const spy = mockFetch(200, {})
    vi.stubGlobal('fetch', spy)
    await apiFetch('/thing', { auth: false, method: 'POST', body: { a: 1 } })
    const init = spy.mock.calls[0][1] as RequestInit
    expect((init.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(init.body).toBe(JSON.stringify({ a: 1 }))
  })

  it('does NOT set content-type for FormData (browser sets boundary)', async () => {
    const spy = mockFetch(200, {})
    vi.stubGlobal('fetch', spy)
    const fd = new FormData()
    fd.append('file', new Blob(['x']), 'x.txt')
    await apiFetch('/files', { auth: false, method: 'POST', body: fd })
    const init = spy.mock.calls[0][1] as RequestInit
    expect((init.headers as Headers).get('Content-Type')).toBeNull()
  })
})
