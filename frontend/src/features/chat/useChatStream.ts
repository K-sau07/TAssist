import { useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { streamMessage } from '@/lib/sse/streamMessage'
import type { SourceItem } from '@/lib/sse/types'

export interface StreamState {
  active: boolean
  mode: string | null
  text: string
  sources: SourceItem[]
  error: string | null
}
const empty: StreamState = { active: false, mode: null, text: '', sources: [], error: null }

/** Manages one in-flight streamed assistant message (§13.4). */
export function useChatStream(streamPath: (chatId: string) => string, chatId: string) {
  const qc = useQueryClient()
  const [state, setState] = useState<StreamState>(empty)
  const abortRef = useRef<AbortController | null>(null)

  async function send(content: string) {
    abortRef.current?.abort()
    const ac = new AbortController()
    abortRef.current = ac
    setState({ ...empty, active: true })

    // optimistically show the user's message via cache
    qc.setQueryData<any>(['chat', chatId], (old: any) => old && {
      ...old,
      messages: [...old.messages, {
        id: `tmp-${Date.now()}`, role: 'USER', content, citations: [], mentionedFiles: [],
        createdAt: new Date().toISOString(),
      }],
    })

    await streamMessage(streamPath(chatId), { content }, {
      onStart: (d) => setState((s) => ({ ...s, mode: d.mode })),
      onSources: (d) => setState((s) => ({ ...s, sources: d.sources })),
      onToken: (d) => setState((s) => ({ ...s, text: s.text + d.text })),
      onError: (d) => setState((s) => ({ ...s, active: false, error: d.message })),
      onDone: () => {
        setState(empty)
        qc.invalidateQueries({ queryKey: ['chat', chatId] })
      },
    }, ac.signal)
  }

  function reset() { setState(empty) }
  return { state, send, reset }
}
