import { useState } from 'react'
import { Sparkles, Trash2 } from 'lucide-react'
import type { MessageView } from '@/lib/api/messaging'
import type { SourceItem } from '@/lib/sse/types'
import { SnippetDrawer } from '@/features/chat/components/SnippetDrawer'
import { timeAgo } from '@/lib/format'

/** One message row: human bubble, AI bubble (with citation chips), or tombstone. */
export function MessageBubble({
  msg, mine, canDelete, onDelete,
}: { msg: MessageView; mine: boolean; canDelete: boolean; onDelete: () => void }) {
  const [source, setSource] = useState<SourceItem | null>(null)

  if (msg.deleted) {
    return (
      <div className="py-1 text-xs italic text-text-faint">message deleted</div>
    )
  }

  const isAi = msg.senderKind === 'AI'

  const openCitation = (i: number) => {
    const c = msg.citations[i]
    if (!c) return
    setSource({ num: i + 1, fileId: c.fileId, label: c.displayLabel, snippet: c.snippet ?? '', similarity: null })
  }

  return (
    <div className={`group flex flex-col ${mine ? 'items-end' : 'items-start'}`}>
      <div className="mb-0.5 flex items-center gap-2 px-1 text-xs text-text-faint">
        {isAi ? (
          <span className="inline-flex items-center gap-1 text-primary"><Sparkles size={12} /> AI</span>
        ) : (
          <span>{mine ? 'You' : msg.sender?.displayName ?? 'Member'}</span>
        )}
        <span>{timeAgo(msg.createdAt)}</span>
        {canDelete && (
          <button onClick={onDelete}
            className="opacity-0 transition-opacity group-hover:opacity-100 hover:text-danger" title="Delete">
            <Trash2 size={12} />
          </button>
        )}
      </div>

      <div className={[
        'max-w-[80%] rounded-lg px-3 py-2 text-sm leading-relaxed whitespace-pre-wrap',
        isAi ? 'border border-primary/30 bg-primary/5'
          : mine ? 'bg-primary text-primary-fg'
            : 'border border-border bg-bg-elev',
      ].join(' ')}>
        {msg.content}
      </div>

      {isAi && msg.citations.length > 0 && (
        <div className="mt-1 flex flex-wrap gap-1 px-1">
          {msg.citations.map((c, i) => (
            <button key={c.chunkId + i} onClick={() => openCitation(i)}
              className="inline-flex h-5 items-center rounded-round bg-primary/12 px-2 text-xs font-medium text-primary hover:bg-primary/20">
              {c.displayLabel}
            </button>
          ))}
        </div>
      )}

      <SnippetDrawer source={source} onClose={() => setSource(null)} />
    </div>
  )
}
