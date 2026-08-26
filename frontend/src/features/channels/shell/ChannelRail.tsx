import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { Users, Plus, Bot, FileText, Settings, ArrowLeft } from 'lucide-react'
import { useChannelContext } from './ChannelShell'
import { useMyDmsQuery, useGroupQuery } from '@/lib/hooks/useMessaging'
import { NewMessagePicker } from '@/features/messaging/NewMessagePicker'
import { Avatar } from '@/features/messaging/Avatar'
import { cn } from '@/lib/cn'

/**
 * Secondary sidebar inside a channel (05_GLOWUP §D4): group room, DMs, section links.
 * The "living sidebar" — unread rows read BOLD, read rows muted; active row carries a
 * 3px indigo left-bar (Slack's at-a-glance unread + location cues).
 */
export function ChannelRail() {
  const { channel, username, isOwner } = useChannelContext()
  const navigate = useNavigate()
  const dms = useMyDmsQuery(channel.id)
  const group = useGroupQuery(channel.id)
  const [picking, setPicking] = useState(false)

  const base = `/c/@${username}`

  // Row style: active = indigo left-bar + sunken + text; unread = bold text; read = muted.
  const row = (opts: { unread?: boolean } = {}) => ({ isActive }: { isActive: boolean }) =>
    cn(
      'relative flex items-center gap-2 rounded-md py-2 pl-4 pr-2 text-sm transition-colors',
      'before:absolute before:left-0 before:top-1/2 before:h-4 before:w-[3px] before:-translate-y-1/2 before:rounded-round before:transition-colors',
      isActive
        ? 'bg-bg-sunken text-text before:bg-primary-rule'
        : opts.unread
          ? 'font-semibold text-text hover:bg-bg-sunken before:bg-transparent'
          : 'text-text-muted hover:bg-bg-sunken hover:text-text before:bg-transparent',
    )

  const sectionCls = 'mb-1 px-2 text-2xs font-semibold uppercase tracking-wider text-text-faint'

  return (
    <aside className="flex h-screen w-[240px] shrink-0 flex-col border-r border-border bg-bg px-3 py-4">
      <button onClick={() => navigate('/app')}
        className="mb-1 flex items-center gap-1 px-2 text-2xs text-text-faint transition-colors hover:text-text">
        <ArrowLeft size={13} /> Home
      </button>
      <div className="px-2 pb-3">
        <h2 className="truncate font-display text-lg leading-tight">{channel.displayName}</h2>
        <p className="truncate text-xs text-text-muted">@{channel.username}</p>
      </div>

      <nav className="flex-1 overflow-y-auto">
        {/* Group room */}
        {group.data && (
          <NavLink to={`${base}/messages/${group.data.id}`} className={row({ unread: group.data.unreadCount > 0 })}>
            <span className="grid h-6 w-6 shrink-0 place-items-center rounded-md bg-primary-wash text-primary">
              <Users size={14} strokeWidth={1.9} />
            </span>
            <span className="flex-1 truncate">Group</span>
            {group.data.unreadCount > 0 && <Badge n={group.data.unreadCount} />}
          </NavLink>
        )}

        {/* DMs */}
        <div className="mb-1 mt-5 flex items-center justify-between px-2">
          <span className="text-2xs font-semibold uppercase tracking-wider text-text-faint">Direct messages</span>
          <button onClick={() => setPicking(true)}
            className="grid h-5 w-5 place-items-center rounded text-text-faint transition-colors hover:bg-bg-sunken hover:text-primary"
            title="New message" aria-label="New message">
            <Plus size={14} strokeWidth={2} />
          </button>
        </div>
        {dms.data && dms.data.length === 0 && (
          <p className="px-2 py-1 text-xs text-text-faint">No conversations yet</p>
        )}
        {dms.data?.map((dm) => (
          <NavLink key={dm.id} to={`${base}/messages/${dm.id}`}
            className={row({ unread: dm.unreadCount > 0 })} title={dm.otherParticipant?.displayName}>
            <Avatar name={dm.otherParticipant?.displayName} size={22} />
            <span className="flex-1 truncate">{dm.otherParticipant?.displayName ?? 'Unknown'}</span>
            {dm.unreadCount > 0 && <Badge n={dm.unreadCount} />}
          </NavLink>
        ))}

        {/* Sections */}
        <div className={cn(sectionCls, 'mt-5')}>Channel</div>
        <NavLink to={`${base}/chat`} className={row()}>
          <span className="grid h-6 w-6 shrink-0 place-items-center"><Bot size={16} strokeWidth={1.75} /></span> AI Chat
        </NavLink>
        <NavLink to={`${base}/files`} className={row()}>
          <span className="grid h-6 w-6 shrink-0 place-items-center"><FileText size={16} strokeWidth={1.75} /></span> Files
        </NavLink>
        {isOwner && (
          <NavLink to={`${base}/manage`} className={row()}>
            <span className="grid h-6 w-6 shrink-0 place-items-center"><Settings size={16} strokeWidth={1.75} /></span> Manage
          </NavLink>
        )}
      </nav>

      {picking && (
        <NewMessagePicker
          channelId={channel.id}
          onClose={() => setPicking(false)}
          onOpened={(id) => { setPicking(false); navigate(`${base}/messages/${id}`) }}
        />
      )}
    </aside>
  )
}

function Badge({ n }: { n: number }) {
  return (
    <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-round bg-primary px-1.5 text-2xs font-semibold text-primary-fg">
      {n > 99 ? '99+' : n}
    </span>
  )
}
