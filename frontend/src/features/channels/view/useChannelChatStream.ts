import { useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { streamMessage } from '@/lib/sse/streamMessage'
import { channelChatStreamPath } from '@/lib/api/channelChat'
import type { StreamState } from '@/features/chat/useChatStream'
import type { SourceItem } from '@/lib/sse/types'

const empty: StreamState = { active: false, mode: null, text: '', sources: [], error: null }

/** Streaming for a channel chat (§14.13) — same event handling as private chat, channel path + query key. */
export function useChannelChatStream(channelId: string, chatId: string) {
  const qc = useQueryClient()
  const [state, setState] = useState<StreamState>(empty)
  const abortRef = useRef<AbortController | null>(null)

  async function send(content: string) {
    abortRef.current?.abort()
    const ac = new AbortController()
    abortRef.current = ac
    setState({ ...empty, active: true })

    qc.setQueryData<any>(['channel-chat', chatId], (old: any) => old && {
      ...old,
      messages: [...old.messages, {
        id: `tmp-${Date.now()}`, role: 'USER', content, createdAt: new Date().toISOString(),
      }],
    })

    await streamMessage(channelChatStreamPath(channelId, chatId), { content }, {
      onStart: (d) => setState((s) => ({ ...s, mode: d.mode })),
      onSources: (d: { sources: SourceItem[] }) => setState((s) => ({ ...s, sources: d.sources })),
      onToken: (d) => setState((s) => ({ ...s, text: s.text + d.text })),
      onError: (d) => setState((s) => ({ ...s, active: false, error: d.message })),
      onDone: () => { setState(empty); qc.invalidateQueries({ queryKey: ['channel-chat', chatId] }) },
    }, ac.signal)
  }
  return { state, send }
}
