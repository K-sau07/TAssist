import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Button } from '@/design/components/Button'
import { useChannelPublicQuery, useRequestJoinMutation } from '@/lib/hooks/useDiscover'
import { Hash } from 'lucide-react'

/**
 * Standalone public channel page for NON-participants (03_CHANNEL_SHELL_SPEC §5).
 * Shows description + "what to expect" + request-access / pending / rejected / banned
 * states. Participants never land here — ChannelShell redirects them into the shell.
 * Wrapped in the global AppLayout only (no channel rail).
 */
export default function ChannelAboutPage() {
  const { handle = '' } = useParams()
  const username = handle.replace(/^@/, '')
  const { data, isLoading } = useChannelPublicQuery(username)
  const requestJoin = useRequestJoinMutation(username)
  const [message, setMessage] = useState('')

  const channel = data?.channel
  const status = data?.myMembershipStatus ?? null

  if (isLoading) return <AppLayout><main className="p-8 text-text-muted">Loading…</main></AppLayout>
  if (!channel) return <AppLayout><main className="p-8 text-text-muted">Channel not found.</main></AppLayout>

  return (
    <AppLayout>
      <main className="mx-auto max-w-2xl px-8 py-10">
        <div className="flex items-center gap-4">
          <div className="grid h-16 w-16 place-items-center rounded-round bg-bg-sunken text-primary">
            <Hash size={28} strokeWidth={1.75} />
          </div>
          <div>
            <h1 className="text-3xl">{channel.displayName}</h1>
            <p className="text-text-muted">@{channel.username} · {channel.visibility.toLowerCase()}</p>
          </div>
        </div>

        {channel.description && (
          <div className="mt-6 rounded-lg border border-border bg-bg-elev p-5">
            <p className="text-sm">{channel.description}</p>
          </div>
        )}
        {channel.expectationSummary && (
          <div className="mt-4 rounded-lg border border-border bg-bg-elev p-5">
            <p className="mb-1 text-xs uppercase tracking-wider text-text-faint">What to expect</p>
            <p className="text-sm">{channel.expectationSummary}</p>
          </div>
        )}

        <div className="mt-8">
          {status === 'PENDING' && (
            <div className="rounded-lg bg-accent-amber/15 p-5 text-sm">Your request is awaiting approval.</div>
          )}
          {(status === null || status === 'REJECTED' || status === 'LEFT') && (
            <div className="rounded-lg border border-border bg-bg-elev p-5">
              <p className="mb-2 text-sm font-medium">Request access</p>
              <textarea rows={2} value={message} onChange={(e) => setMessage(e.target.value)}
                placeholder="Add a short message (optional)"
                className="mb-3 w-full rounded-md border border-border bg-bg px-3 py-2 text-sm outline-none focus:border-primary" />
              <Button
                disabled={requestJoin.isPending}
                onClick={() => requestJoin.mutate({ channelId: channel.id, message: message.trim() || undefined })}>
                {requestJoin.isPending ? 'Requesting…' : 'Request access'}
              </Button>
            </div>
          )}
          {status === 'BANNED' && (
            <div className="rounded-lg bg-danger/10 p-5 text-sm text-danger">You cannot access this channel.</div>
          )}
        </div>
      </main>
    </AppLayout>
  )
}
