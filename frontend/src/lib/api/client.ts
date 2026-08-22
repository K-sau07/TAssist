// Base fetch wrapper: injects auth header, maps errors to a typed ApiError (spec §13, §17.4).
import { useAuthStore } from '@/lib/auth/store'

export class ApiError extends Error {
  status: number
  code?: string
  retryAfterSeconds?: number
  details?: Record<string, string>
  constructor(status: number, message: string, opts?: {
    code?: string; retryAfterSeconds?: number; details?: Record<string, string>
  }) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = opts?.code
    this.retryAfterSeconds = opts?.retryAfterSeconds
    this.details = opts?.details
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
    let details: Record<string, string> | undefined
    try {
      const data = await res.json()
      // §17.4 envelope: { error: { code, message, details, correlationId } }
      const err = data?.error ?? data
      code = err?.code
      message = err?.message ?? message
      details = err?.details ?? undefined
      // retryAfterSeconds lives inside details for RATE_LIMITED
      const ra = details?.retryAfterSeconds ?? err?.retryAfterSeconds
      if (ra != null) retryAfterSeconds = Number(ra)
    } catch { /* non-JSON error body */ }
    throw new ApiError(res.status, message, { code, retryAfterSeconds, details })
  }

  if (res.status === 204) return undefined as T
  const ct = res.headers.get('content-type') ?? ''
  return (ct.includes('application/json') ? await res.json() : (await res.text())) as T
}
