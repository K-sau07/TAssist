import { useAuthStore } from '@/lib/auth/store'

// Realtime conversation events (02_MESSAGING_SPEC §8). The messaging stream is a
// long-lived GET (unlike the POST RAG token stream) that pushes message/deleted/read
// events. EventSource can't send Authorization headers, so we use fetch + ReadableStream.

export interface ConvMessageEvent {
  messageId: string
  conversationId: string
  senderKind: 'HUMAN' | 'AI'
  senderId: string | null
  content: string
  createdAt: string
}
export interface ConvDeletedEvent { messageId: string }
export interface ConvReadEvent { userId: string; at: string }

export interface ConversationStreamHandlers {
  onMessage?: (e: ConvMessageEvent) => void
  onDeleted?: (e: ConvDeletedEvent) => void
  onRead?: (e: ConvReadEvent) => void
  onError?: (e: { code: string; message: string }) => void
}

/**
 * Subscribe to a conversation's SSE stream. Returns nothing; pass an AbortSignal to stop.
 * A post-abort connection close is expected and never surfaced as an error (mirrors the
 * hardened RAG client — a clean unsubscribe must not look like a dropped connection).
 */
export async function subscribeConversation(
  path: string,
  handlers: ConversationStreamHandlers,
  signal: AbortSignal,
): Promise<void> {
  const token = useAuthStore.getState().token
  let res: Response
  try {
    res = await fetch(`/api${path}`, {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      signal,
    })
  } catch (e) {
    if ((e as Error)?.name === 'AbortError') return // unsubscribed — not an error
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

  const dispatch = (frame: string) => {
    const ev = parseEvent(frame)
    if (!ev) return
    switch (ev.event) {
      case 'message': handlers.onMessage?.(ev.data as ConvMessageEvent); break
      case 'deleted': handlers.onDeleted?.(ev.data as ConvDeletedEvent); break
      case 'read': handlers.onRead?.(ev.data as ConvReadEvent); break
    }
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
    if (buffer.trim()) dispatch(buffer)
  } catch (e) {
    if ((e as Error)?.name === 'AbortError') return // unsubscribed cleanly — expected close
    handlers.onError?.({ code: 'CONNECTION_LOST', message: 'The connection dropped.' })
  }
}

/** Parse one SSE frame into {event,data}. Ignores keep-alive ping comments. Exported for tests. */
export function parseEvent(frame: string): { event: string; data: unknown } | null {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of frame.split('\n')) {
    if (line.startsWith(':')) continue // keep-alive ping
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  if (dataLines.length === 0) return null
  try { return { event, data: JSON.parse(dataLines.join('\n')) } } catch { return null }
}
