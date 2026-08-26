import { useEffect, useRef, useState } from 'react'
import { Hash } from 'lucide-react'
import { useParticipantsQuery, useOpenDmMutation } from '@/lib/hooks/useMessaging'

function OwnerTag() {
  return <span className="rounded-round bg-accent-amber/20 px-1.5 text-[10px] uppercase tracking-wide text-accent-amber">Owner</span>
}

/**
 * Modal picker to start a DM with any approved participant (+owner), minus self.
 * Shared by the channel rail and the messaging home. Idempotent open-or-create.
 */
export function NewMessagePicker({
  channelId, onClose, onOpened,
}: { channelId: string; onClose: () => void; onOpened: (conversationId: string) => void }) {
  const { data: people = [], isLoading } = useParticipantsQuery(channelId)
  const openDm = useOpenDmMutation(channelId)
  const [q, setQ] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const filtered = people.filter((p) => p.displayName.toLowerCase().includes(q.toLowerCase()))

  async function pick(userId: string) {
    const conv = await openDm.mutateAsync(userId)
    onOpened(conv.id)
  }

  useEffect(() => { inputRef.current?.focus() }, [])

  return (
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div className="fixed inset-0 z-40 flex items-start justify-center bg-black/30 p-4 pt-24"
      role="dialog" aria-modal="true" aria-label="New message"
      onClick={onClose} onKeyDown={(e) => { if (e.key === 'Escape') onClose() }} tabIndex={-1}>
      {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions */}
      <div className="w-full max-w-md rounded-lg border border-border bg-bg-elev p-5 shadow-2"
        role="document" onClick={(e) => e.stopPropagation()}>
        <div className="mb-3 flex items-center gap-2">
          <Hash size={16} className="text-text-faint" />
          <input ref={inputRef} value={q} onChange={(e) => setQ(e.target.value)}
            placeholder="Search people…"
            className="flex-1 bg-transparent text-sm outline-none" />
          <button onClick={onClose} className="text-xs text-text-faint hover:text-text">Esc</button>
        </div>
        <div className="max-h-72 space-y-1 overflow-y-auto">
          {isLoading && <p className="px-2 py-3 text-sm text-text-muted">Loading…</p>}
          {!isLoading && filtered.length === 0 && <p className="px-2 py-3 text-sm text-text-muted">No people found.</p>}
          {filtered.map((p) => (
            <button key={p.userId} disabled={openDm.isPending} onClick={() => pick(p.userId)}
              className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left text-sm hover:bg-bg-sunken">
              <span className="grid h-8 w-8 place-items-center rounded-round bg-bg-sunken text-xs font-medium">
                {p.displayName.charAt(0).toUpperCase()}
              </span>
              <span className="flex-1">{p.displayName}</span>
              {p.isOwner && <OwnerTag />}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
