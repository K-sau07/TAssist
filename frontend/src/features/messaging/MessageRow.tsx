import { useState } from 'react'
import { Sparkles, Trash2 } from 'lucide-react'
import type { MessageView } from '@/lib/api/messaging'
import type { SourceItem } from '@/lib/sse/types'
import { SnippetDrawer } from '@/features/chat/components/SnippetDrawer'
import { timeAgo } from '@/lib/format'
import { Avatar } from './Avatar'

const GUTTER = 44 // px — avatar column width; grouped rows indent body to match

/**
 * One message in the Slack-style thread (05_GLOWUP §D5, G-UI1).
 * - `grouped`: collapses under the previous same-author message (no avatar/name;
 *   body indented into the gutter; timestamp appears on hover at the left edge).
 * - Hover reveals an action toolbar (delete now; react/reply later).
 * - AI messages get a light placeholder here; G-UI2 replaces it with the margin-note.
 */
export function MessageRow({
  msg, grouped, canDelete, onDelete,
}: { msg: MessageView; grouped: boolean; canDelete: boolean; onDelete: () => void }) {
  const [source, setSource] = useState<SourceItem | null>(null)
  const isAi = msg.senderKind === 'AI'

  const time = new Date(msg.createdAt).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })

  if (msg.deleted) {
    return (
      <div className="group flex items-start gap-2 rounded-md px-2 py-0.5 hover:bg-bg-sunken/60"
        style={{ paddingLeft: grouped ? GUTTER + 8 : 8 }}>
        <span className="py-1 text-xs italic text-text-faint">This message was deleted</span>
      </div>
    )
  }

  const openCitation = (i: number) => {
    const c = msg.citations[i]
    if (!c) return
    setSource({ num: i + 1, fileId: c.fileId, label: c.displayLabel, snippet: c.snippet ?? '', similarity: null })
  }

  return (
    <div className="group relative flex gap-2 rounded-md px-2 transition-colors hover:bg-bg-sunken/50"
      style={{ paddingTop: grouped ? 2 : 8, paddingBottom: 2 }}>
      {/* gutter: avatar (ungrouped) or hover-timestamp (grouped) */}
      <div className="flex shrink-0 justify-center" style={{ width: GUTTER - 8 }}>
        {grouped ? (
          <span className="mt-0.5 select-none text-[10px] leading-5 text-text-faint opacity-0 transition-opacity group-hover:opacity-100">
            {time}
          </span>
        ) : (
          <Avatar name={msg.sender?.displayName} ai={isAi} size={GUTTER - 8} />
        )}
      </div>

      {/* content */}
      <div className="min-w-0 flex-1">
        {!grouped && (
          <div className="mb-0.5 flex items-baseline gap-2">
            <span className="text-sm font-semibold text-text">
              {isAi ? (
                <span className="inline-flex items-center gap-1 text-primary"><Sparkles size={13} /> AI</span>
              ) : (msg.sender?.displayName ?? 'Member')}
            </span>
            <span className="text-2xs text-text-faint">{timeAgo(msg.createdAt)}</span>
          </div>
        )}

        <div className="whitespace-pre-wrap break-words text-md leading-relaxed text-text">
          {msg.content}
        </div>

        {isAi && msg.citations.length > 0 && (
          <div className="mt-1.5 flex flex-wrap gap-1">
            {msg.citations.map((c, i) => (
              <button key={c.chunkId + i} onClick={() => openCitation(i)}
                className="inline-flex h-6 items-center gap-1 rounded-round bg-primary-wash px-2 text-2xs font-medium text-primary transition-colors hover:bg-primary-wash-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus">
                <sup className="font-mono">{i + 1}</sup>{c.displayLabel}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* hover action toolbar */}
      {canDelete && (
        <div className="absolute right-2 top-0 -translate-y-1/2 opacity-0 transition-opacity group-hover:opacity-100">
          <button onClick={onDelete} title="Delete message"
            className="grid h-7 w-7 place-items-center rounded-md border border-border bg-bg-elev text-text-faint shadow-1 hover:text-danger focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus">
            <Trash2 size={13} />
          </button>
        </div>
      )}

      <SnippetDrawer source={source} onClose={() => setSource(null)} />
    </div>
  )
}
