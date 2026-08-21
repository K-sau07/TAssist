// Base fetch wrapper: injects auth header, maps errors to a typed ApiError (spec §13).
import { useAuthStore } from '@/lib/auth/store'

export class ApiError extends Error {
  status: number
  code?: string
  retryAfterSeconds?: number
  constructor(status: number, message: string, code?: string, retryAfterSeconds?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.retryAfterSeconds = retryAfterSeconds
  }
}

type Options = Omit<RequestInit, 'body'> & { body?: unknown; auth?: boolean }

export async function apiFetch<T = unknown>(path: string, opts: Options = {}): Promise<T> {
  const { body, auth = true, headers, ...rest } = opts
  const h = new Headers(headers)
  if (auth) {
    const token = useAuthStore.getState().token
    if (token) h.set('Authorization', `Bearer ${token}`)
  }
  let payload: BodyInit | undefined
  if (body !== undefined) {
    if (body instanceof FormData) {
      payload = body // let the browser set the multipart boundary
    } else {
      h.set('Content-Type', 'application/json')
      payload = JSON.stringify(body)
    }
  }

  const res = await fetch(`/api${path}`, { ...rest, headers: h, body: payload })

  if (res.status === 401 && auth) {
    // token invalid/expired — clear and let guards bounce to /login
    useAuthStore.getState().clear()
  }

  if (!res.ok) {
    let code: string | undefined
    let message = `Request failed (${res.status})`
    let retryAfterSeconds: number | undefined
    try {
      const data = await res.json()
      code = data.code
      message = data.message ?? message
      retryAfterSeconds = data.retryAfterSeconds
    } catch { /* non-JSON error body */ }
    throw new ApiError(res.status, message, code, retryAfterSeconds)
  }

  if (res.status === 204) return undefined as T
  const ct = res.headers.get('content-type') ?? ''
  return (ct.includes('application/json') ? await res.json() : (await res.text())) as T
}
