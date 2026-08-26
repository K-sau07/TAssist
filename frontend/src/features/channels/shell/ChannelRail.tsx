import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { Users, Plus, Bot, FileText, Settings, ArrowLeft } from 'lucide-react'
import { useChannelContext } from './ChannelShell'
import { useMyDmsQuery, useGroupQuery } from '@/lib/hooks/useMessaging'
import { NewMessagePicker } from '@/features/messaging/NewMessagePicker'
import { cn } from '@/lib/cn'

/** Secondary sidebar inside a channel: group room, DMs, and section links. */
export function ChannelRail() {
  const { channel, username, isOwner } = useChannelContext()
  const navigate = useNavigate()
  const dms = useMyDmsQuery(channel.id)
  const group = useGroupQuery(channel.id)
  const [picking, setPicking] = useState(false)

  const base = `/c/@${username}`
  const rowCls = ({ isActive }: { isActive: boolean }) =>
    cn('flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors',
       isActive ? 'bg-bg-sunken font-medium text-text' : 'text-text-muted hover:bg-bg-sunken')

  return (
    <aside className="flex h-screen w-[240px] shrink-0 flex-col border-r border-border bg-bg px-3 py-4">
      <button onClick={() => navigate('/app')}
        className="mb-1 flex items-center gap-1 px-2 text-xs text-text-faint hover:text-text">
        <ArrowLeft size={13} /> Home
      </button>
      <div className="px-2 pb-3">
        <h2 className="truncate font-display text-lg">{channel.displayName}</h2>
        <p className="truncate text-xs text-text-muted">@{channel.username}</p>
      </div>

      <nav className="flex-1 overflow-y-auto">
        {/* Group room */}
        {group.data && (
          <NavLink to={`${base}/messages/${group.data.id}`} className={rowCls}>
            <Users size={16} strokeWidth={1.75} />
            <span className="flex-1 truncate"># Group</span>
            {group.data.unreadCount > 0 && <Badge n={group.data.unreadCount} />}
          </NavLink>
        )}

        {/* DMs */}
        <div className="mb-1 mt-4 flex items-center justify-between px-3">
          <span className="text-xs uppercase tracking-wider text-text-faint">Direct messages</span>
          <button onClick={() => setPicking(true)} className="text-text-faint hover:text-primary" title="New message">
            <Plus size={15} strokeWidth={1.75} />
          </button>
        </div>
        {dms.data && dms.data.length === 0 && (
          <p className="px-3 py-1 text-xs text-text-faint">No conversations yet</p>
        )}
        {dms.data?.map((dm) => (
          <NavLink key={dm.id} to={`${base}/messages/${dm.id}`} className={rowCls} title={dm.otherParticipant?.displayName}>
            <span className="grid h-5 w-5 shrink-0 place-items-center rounded-round bg-bg-sunken text-[10px] font-medium">
              {(dm.otherParticipant?.displayName ?? '?').charAt(0).toUpperCase()}
            </span>
            <span className="flex-1 truncate">{dm.otherParticipant?.displayName ?? 'Unknown'}</span>
            {dm.unreadCount > 0 && <Badge n={dm.unreadCount} />}
          </NavLink>
        ))}

        {/* Sections */}
        <div className="mb-1 mt-5 px-3 text-xs uppercase tracking-wider text-text-faint">Channel</div>
        <NavLink to={`${base}/chat`} className={rowCls}>
          <Bot size={16} strokeWidth={1.75} /> AI Chat
        </NavLink>
        <NavLink to={`${base}/files`} className={rowCls}>
          <FileText size={16} strokeWidth={1.75} /> Files
        </NavLink>
        {isOwner && (
          <NavLink to={`${base}/manage`} className={rowCls}>
            <Settings size={16} strokeWidth={1.75} /> Manage
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
    <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-round bg-primary px-1.5 text-xs font-medium text-white">
      {n > 99 ? '99+' : n}
    </span>
  )
}
