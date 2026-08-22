import { useAuthStore } from '@/lib/auth/store'
import type { StreamHandlers } from './types'

/**
 * POST to an SSE endpoint and dispatch events to handlers (§13.4).
 * Uses fetch + ReadableStream because EventSource cannot send Authorization headers.
 */
export async function streamMessage(
  path: string,
  body: unknown,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const token = useAuthStore.getState().token
  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(body),
      signal,
    })
  } catch (e) {
    if ((e as Error)?.name === 'AbortError') return // user navigated away — not an error
    handlers.onError?.({ code: 'CONNECTION_LOST', message: 'Could not reach the server.' })
    return
  }

  if (res.status === 401) { useAuthStore.getState().clear() }
  if (!res.ok || !res.body) {
    handlers.onError?.({ code: `HTTP_${res.status}`, message: `Stream failed (${res.status}).` })
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  // Once the server signals completion (done/error), the exchange is over.
  // Any subsequent connection-close exception is expected and must NOT be
  // surfaced as an error — otherwise a perfectly good answer shows "connection dropped".
  let finished = false

  const dispatch = (frame: string) => {
    const ev = parseEvent(frame)
    if (!ev) return
    if (ev.event === 'done' || ev.event === 'error') finished = true
    handle(ev, handlers)
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      let sep: number
      while ((sep = buffer.indexOf('\n\n')) !== -1) {
        const frame = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        dispatch(frame)
      }
    }
    // flush any trailing frame not terminated by a blank line
    if (buffer.trim()) dispatch(buffer)
  } catch (e) {
    if ((e as Error)?.name === 'AbortError') return // navigated away
    // If the stream already completed, a close-time exception is expected — ignore it.
    if (!finished) {
      handlers.onError?.({ code: 'CONNECTION_LOST', message: 'The connection dropped.' })
    }
  }
}

function parseEvent(frame: string): { event: string; data: any } | null {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of frame.split('\n')) {
    if (line.startsWith(':')) continue // keep-alive ping comment
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  if (dataLines.length === 0) return null
  try { return { event, data: JSON.parse(dataLines.join('\n')) } } catch { return null }
}

function handle({ event, data }: { event: string; data: any }, h: StreamHandlers) {
  switch (event) {
    case 'start': h.onStart?.(data); break
    case 'sources': h.onSources?.(data); break
    case 'token': h.onToken?.(data); break
    case 'tool_use': h.onToolUse?.(data); break
    case 'tool_result': h.onToolResult?.(data); break
    case 'citation': h.onCitation?.(data); break
    case 'done': h.onDone?.(data); break
    case 'error': h.onError?.(data); break
  }
}
