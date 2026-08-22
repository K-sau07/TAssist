import { useEffect, useRef } from 'react'
import type { MessageView } from '@/lib/api/chats'
import type { SourceItem } from '@/lib/sse/types'
import { MessageContent } from './MessageContent'
import { SourcesStrip } from './SourcesStrip'
import { FallbackPill } from './FallbackPill'
import type { StreamState } from '../useChatStream'

export function MessageList({
  messages, stream, onCite,
}: { messages: MessageView[]; stream: StreamState; onCite: (s: SourceItem) => void }) {
  const bottomRef = useRef<HTMLDivElement>(null)
  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) },
    [messages.length, stream.text])

  const citeFromMessage = (m: MessageView, num: number) => {
    const c = m.citations[num - 1]
    if (c) onCite({ num, fileId: c.fileId, label: c.label, similarity: null, snippet: c.snippet ?? '' })
  }

  return (
    <div className="flex-1 space-y-6 overflow-y-auto px-6 py-8">
      {messages.map((m) => (
        <div key={m.id} className={m.role === 'USER' ? 'flex justify-end' : 'flex justify-start'}>
          <div className={m.role === 'USER'
            ? 'max-w-[80%] rounded-lg rounded-tr-sm bg-primary px-4 py-3 text-primary-fg'
            : 'max-w-[80%] rounded-lg rounded-tl-sm bg-bg-elev border border-border px-4 py-3'}>
            {m.role === 'ASSISTANT'
              ? <MessageContent text={m.content} onCite={(n) => citeFromMessage(m, n)} />
              : <span className="whitespace-pre-wrap">{m.content}</span>}
          </div>
        </div>
      ))}

      {stream.active && (
        <div className="flex justify-start">
          <div className="max-w-[80%] rounded-lg rounded-tl-sm border border-border bg-bg-elev px-4 py-3">
            {stream.mode === 'fallback' && <FallbackPill />}
            <SourcesStrip sources={stream.sources}
              onOpen={(num) => { const s = stream.sources.find((x) => x.num === num); if (s) onCite(s) }} />
            {stream.text
              ? <MessageContent text={stream.text}
                  onCite={(num) => { const s = stream.sources.find((x) => x.num === num); if (s) onCite(s) }} />
              : <span className="text-text-faint">Thinking…</span>}
            <span className="ml-1 inline-block h-4 w-1.5 animate-pulse rounded-round bg-primary align-middle" />
          </div>
        </div>
      )}

      {stream.error && (
        <div className="rounded-md bg-danger/10 px-4 py-3 text-sm text-danger">{stream.error}</div>
      )}
      <div ref={bottomRef} />
    </div>
  )
}
