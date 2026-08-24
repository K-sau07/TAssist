import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Button } from '@/design/components/Button'
import { useChannelPublicQuery } from '@/lib/hooks/useDiscover'
import {
  useMyDmsQuery, useGroupQuery, useParticipantsQuery, useOpenDmMutation,
} from '@/lib/hooks/useMessaging'
import { timeAgo } from '@/lib/format'
import { Hash, Users, Plus, MessageSquare } from 'lucide-react'

export default function MessagesHomePage() {
  const { handle = '' } = useParams()
  const username = handle.replace(/^@/, '')
  const navigate = useNavigate()
  const { data, isLoading } = useChannelPublicQuery(username)
  const channel = data?.channel
  const status = data?.myMembershipStatus ?? null
  const canUse = status === 'APPROVED' || status === 'OWNER'
  const channelId = channel?.id ?? ''

  const dms = useMyDmsQuery(channelId, canUse)
  const group = useGroupQuery(channelId, canUse)
  const [picking, setPicking] = useState(false)

  const go = (conversationId: string) =>
    navigate(`/c/@${username}/messages/${conversationId}`)

  if (isLoading) return <AppLayout><main className="p-8 text-text-muted">Loading…</main></AppLayout>
  if (!channel) return <AppLayout><main className="p-8 text-text-muted">Channel not found.</main></AppLayout>
  if (!canUse) {
    return (
      <AppLayout>
        <main className="mx-auto max-w-2xl px-8 py-10 text-text-muted">
          You need to be an approved member to message in this channel.
        </main>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <main className="mx-auto max-w-2xl px-8 py-10">
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-2xl">Messages</h1>
            <p className="text-sm text-text-muted">@{channel.username}</p>
          </div>
          <Button onClick={() => setPicking(true)}>
            <Plus size={16} strokeWidth={1.75} /> New message
          </Button>
        </div>

        {/* group room */}
        {group.data && (
          <button
            onClick={() => go(group.data!.id)}
            className="mb-3 flex w-full items-center gap-3 rounded-lg border border-border bg-bg-elev px-4 py-3 text-left hover:border-primary"
          >
            <div className="grid h-9 w-9 place-items-center rounded-round bg-primary/12 text-primary">
              <Users size={18} strokeWidth={1.75} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="flex items-center gap-2 text-sm font-medium">
                # Group
                {group.data.unreadCount > 0 && <UnreadBadge n={group.data.unreadCount} />}
              </p>
              <p className="truncate text-xs text-text-muted">
                {group.data.lastMessagePreview ?? 'Everyone in the channel'}
              </p>
            </div>
          </button>
        )}

        {/* DM inbox */}
        <p className="mb-2 mt-6 text-xs uppercase tracking-wider text-text-faint">Direct messages</p>
        {dms.data && dms.data.length === 0 && (
          <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-text-muted">
            <MessageSquare size={20} className="mx-auto mb-2 text-text-faint" />
            No conversations yet. Start one with “New message”.
          </div>
        )}
        <div className="space-y-2">
          {dms.data?.map((dm) => (
            <button key={dm.id} onClick={() => go(dm.id)}
              className="flex w-full items-center gap-3 rounded-lg border border-border bg-bg-elev px-4 py-3 text-left hover:border-primary">
              <div className="grid h-9 w-9 place-items-center rounded-round bg-bg-sunken text-sm font-medium">
                {(dm.otherParticipant?.displayName ?? '?').charAt(0).toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="flex items-center gap-2 text-sm font-medium">
                  {dm.otherParticipant?.displayName ?? 'Unknown'}
                  {dm.otherParticipant?.isOwner && <OwnerTag />}
                  {dm.unreadCount > 0 && <UnreadBadge n={dm.unreadCount} />}
                </p>
                <p className="truncate text-xs text-text-muted">{dm.lastMessagePreview ?? 'No messages yet'}</p>
              </div>
              <span className="shrink-0 text-xs text-text-faint">{timeAgo(dm.updatedAt)}</span>
            </button>
          ))}
        </div>
      </main>

      {picking && (
        <NewMessagePicker
          channelId={channelId}
          onClose={() => setPicking(false)}
          onOpened={(id) => { setPicking(false); go(id) }}
        />
      )}
    </AppLayout>
  )
}

function UnreadBadge({ n }: { n: number }) {
  return (
    <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-round bg-primary px-1.5 text-xs font-medium text-white">
      {n > 99 ? '99+' : n}
    </span>
  )
}
function OwnerTag() {
  return <span className="rounded-round bg-accent-amber/20 px-1.5 text-[10px] uppercase tracking-wide text-accent-amber">Owner</span>
}

function NewMessagePicker({
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

  // Focus the search box on open without the a11y-flagged autoFocus prop.
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
