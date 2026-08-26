import { Trash2 } from 'lucide-react'
import type { MessageView } from '@/lib/api/messaging'
import { timeAgo } from '@/lib/format'
import { Avatar } from './Avatar'

const GUTTER = 44 // px — avatar column width; grouped rows indent body to match

/**
 * One HUMAN message in the Slack-style thread (05_GLOWUP §D5, G-UI1).
 * (AI turns render as <AiMarginNote/> instead — routed in ThreadPage.)
 * - `grouped`: collapses under the previous same-author message (no avatar/name;
 *   body indented; timestamp appears on hover at the left edge).
 * - Hover reveals an action toolbar (delete now; react/reply later).
 */
export function MessageRow({
  msg, grouped, canDelete, onDelete,
}: { msg: MessageView; grouped: boolean; canDelete: boolean; onDelete: () => void }) {
  const time = new Date(msg.createdAt).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })

  if (msg.deleted) {
    return (
      <div className="group flex items-start gap-2 rounded-md px-2 py-0.5 hover:bg-bg-sunken/60"
        style={{ paddingLeft: grouped ? GUTTER + 8 : 8 }}>
        <span className="py-1 text-xs italic text-text-faint">This message was deleted</span>
      </div>
    )
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
          <Avatar name={msg.sender?.displayName} size={GUTTER - 8} />
        )}
      </div>

      {/* content */}
      <div className="min-w-0 flex-1">
        {!grouped && (
          <div className="mb-0.5 flex items-baseline gap-2">
            <span className="text-sm font-semibold text-text">{msg.sender?.displayName ?? 'Member'}</span>
            <span className="text-2xs text-text-faint">{timeAgo(msg.createdAt)}</span>
          </div>
        )}

        <div className="whitespace-pre-wrap break-words text-md leading-relaxed text-text">
          {msg.content}
        </div>
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
    </div>
  )
}
