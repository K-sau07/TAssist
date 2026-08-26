import { createContext, useContext } from 'react'
import { Outlet, useParams, Navigate } from 'react-router-dom'
import { LeftRail } from '@/features/dashboard/shell/LeftRail'
import { ChannelRail } from './ChannelRail'
import { useChannelPublicQuery } from '@/lib/hooks/useDiscover'
import type { ChannelView, MembershipStatus } from '@/lib/api/channels'

export type ChannelStatus = MembershipStatus | 'OWNER' | null

interface ChannelContextValue {
  channel: ChannelView
  username: string
  status: ChannelStatus
  isOwner: boolean
  canUse: boolean // owner or approved — may see messaging + AI chat
}

const ChannelContext = createContext<ChannelContextValue | null>(null)

/** Access the resolved channel + membership inside any channel sub-view. */
export function useChannelContext(): ChannelContextValue {
  const ctx = useContext(ChannelContext)
  if (!ctx) throw new Error('useChannelContext must be used within <ChannelShell/>')
  return ctx
}

/**
 * Unified channel shell (Slack-style). Everything under /c/:handle renders here:
 * global rail + channel secondary rail (group, DMs, sections) + the active surface.
 * Owner and approved members share the same shell; the rail hides owner-only links.
 */
export default function ChannelShell() {
  const { handle = '' } = useParams()
  const username = handle.replace(/^@/, '')
  const { data, isLoading } = useChannelPublicQuery(username)

  if (isLoading) {
    return (
      <div className="flex h-screen bg-bg">
        <LeftRail />
        <div className="flex-1 p-8 text-text-muted">Loading…</div>
      </div>
    )
  }
  if (!data?.channel) {
    return (
      <div className="flex h-screen bg-bg">
        <LeftRail />
        <div className="flex-1 p-8 text-text-muted">Channel not found.</div>
      </div>
    )
  }

  const channel = data.channel
  const status: ChannelStatus = data.myMembershipStatus ?? null
  const isOwner = status === 'OWNER'
  const canUse = isOwner || status === 'APPROVED'

  // Not a participant → they can't see the shell; send to the public about/request page.
  if (!canUse) return <Navigate to={`/c/@${username}/about`} replace />

  const value: ChannelContextValue = { channel, username, status, isOwner, canUse }

  return (
    <ChannelContext.Provider value={value}>
      <div className="flex h-screen bg-bg">
        <LeftRail />
        <ChannelRail />
        <div className="flex-1 overflow-hidden">
          <Outlet />
        </div>
      </div>
    </ChannelContext.Provider>
  )
}
