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
  } catch {
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

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE frames separated by a blank line
      let sep: number
      while ((sep = buffer.indexOf('\n\n')) !== -1) {
        const frame = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        dispatchFrame(frame, handlers)
      }
    }
  } catch {
    handlers.onError?.({ code: 'CONNECTION_LOST', message: 'The connection dropped.' })
  }
}

function dispatchFrame(frame: string, h: StreamHandlers) {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of frame.split('\n')) {
    if (line.startsWith(':')) continue // keep-alive ping comment
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  if (dataLines.length === 0) return
  let data: any
  try { data = JSON.parse(dataLines.join('\n')) } catch { return }

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
