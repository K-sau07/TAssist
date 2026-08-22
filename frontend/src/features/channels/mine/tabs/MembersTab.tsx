import { useState } from 'react'
import { Button } from '@/design/components/Button'
import { useMembersQuery, useMemberActionMutation } from '@/lib/hooks/useChannels'
import type { MembershipStatus } from '@/lib/api/channels'
import { timeAgo } from '@/lib/format'

const STATUSES: MembershipStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'BANNED', 'LEFT']

export function MembersTab({ channelId }: { channelId: string }) {
  const [status, setStatus] = useState<MembershipStatus>('PENDING')
  const { data: members = [], isLoading } = useMembersQuery(channelId, status)
  const action = useMemberActionMutation(channelId)

  return (
    <div>
      <div className="mb-4 flex gap-1">
        {STATUSES.map((s) => (
          <button key={s} onClick={() => setStatus(s)}
            className={`rounded-round px-3 py-1 text-sm ${status === s ? 'bg-primary text-primary-fg' : 'bg-bg-sunken text-text-muted hover:text-text'}`}>
            {s.charAt(0) + s.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {isLoading ? <p className="text-text-muted">Loading…</p>
        : members.length === 0 ? <p className="text-sm text-text-muted">No {status.toLowerCase()} members.</p>
        : (
          <div className="space-y-2">
            {members.map((m) => (
              <div key={m.id} className="flex items-center justify-between rounded-md border border-border bg-bg-elev px-4 py-3">
                <div className="min-w-0">
                  <p className="font-mono text-sm">{m.userId.slice(0, 8)}…</p>
                  {m.requestMessage && <p className="mt-0.5 truncate text-sm text-text-muted" title={m.requestMessage}>“{m.requestMessage}”</p>}
                  <p className="text-xs text-text-faint">Requested {timeAgo(m.createdAt)}</p>
                </div>
                <div className="flex shrink-0 gap-2">
                  {status === 'PENDING' && <>
                    <Button size="sm" onClick={() => action.mutate({ action: 'approve', membershipId: m.id })}>Approve</Button>
                    <Button size="sm" variant="ghost" onClick={() => action.mutate({ action: 'deny', membershipId: m.id })}>Deny</Button>
                  </>}
                  {status === 'APPROVED' && <>
                    <Button size="sm" variant="ghost" onClick={() => action.mutate({ action: 'kick', membershipId: m.id })}>Kick</Button>
                    <Button size="sm" variant="danger" onClick={() => action.mutate({ action: 'ban', membershipId: m.id })}>Ban</Button>
                  </>}
                </div>
              </div>
            ))}
          </div>
        )}
    </div>
  )
}
