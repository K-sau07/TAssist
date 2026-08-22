import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Button } from '@/design/components/Button'
import { useChannelPublicQuery, useRequestJoinMutation } from '@/lib/hooks/useDiscover'
import { listChannelChats, createChannelChat } from '@/lib/api/channelChat'
import { Hash } from 'lucide-react'

export default function ChannelLandingPage() {
  const { username = '' } = useParams()
  const navigate = useNavigate()
  const { data, isLoading } = useChannelPublicQuery(username)
  const requestJoin = useRequestJoinMutation(username)
  const [message, setMessage] = useState('')

  const channel = data?.channel
  const status = data?.myMembershipStatus ?? null

  const { data: myChats = [] } = useQuery({
    queryKey: ['channel', channel?.id, 'my-chats'],
    queryFn: () => listChannelChats(channel!.id),
    enabled: Boolean(channel?.id) && status === 'APPROVED',
  })

  async function openNewChat() {
    if (!channel) return
    const chat = await createChannelChat(channel.id)
    navigate(`/c/@${username}/chats/${chat.id}`)
  }

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

        {/* access panel */}
        <div className="mt-8">
          {status === 'OWNER' && (
            <div className="rounded-lg bg-bg-sunken p-5 text-sm text-text-muted">
              You own this channel. <Button variant="ghost" onClick={() => navigate(`/app/channels/${channel.id}/manage`)}>Manage it</Button>
            </div>
          )}
          {status === 'APPROVED' && (
            <div>
              <Button onClick={openNewChat}>Open chat</Button>
              {myChats.length > 0 && (
                <div className="mt-4">
                  <p className="mb-2 text-sm text-text-muted">Your chats in this channel</p>
                  <div className="space-y-2">
                    {myChats.map((c) => (
                      <button key={c.id} onClick={() => navigate(`/c/@${username}/chats/${c.id}`)}
                        className="block w-full rounded-md border border-border bg-bg-elev px-4 py-2 text-left text-sm hover:border-primary">
                        {c.title}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
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
