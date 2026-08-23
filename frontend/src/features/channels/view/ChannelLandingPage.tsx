import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Button } from '@/design/components/Button'
import { useChannelPublicQuery, useRequestJoinMutation } from '@/lib/hooks/useDiscover'
import { listChannelChats, deleteChannelChat } from '@/lib/api/channelChat'
import { Hash, Plus, Trash2 } from 'lucide-react'

export default function ChannelLandingPage() {
  const { handle = '' } = useParams()
  const username = handle.replace(/^@/, '')
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

  const qc = useQueryClient()
  const delChat = useMutation({
    mutationFn: (chatId: string) => deleteChannelChat(channel!.id, chatId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channel', channel?.id, 'my-chats'] }),
  })

  // Deferred creation: don't create a chat until the first message is sent (no blank chats).
  function openNewChat() {
    navigate(`/c/@${username}/chats/new`)
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
              <Button onClick={openNewChat}><Plus size={16} strokeWidth={1.75} /> New chat</Button>
              {myChats.length > 0 && (
                <div className="mt-4">
                  <p className="mb-2 text-sm text-text-muted">Your chats in this channel</p>
                  <div className="space-y-2">
                    {myChats.map((c) => (
                      <div key={c.id} className="group flex items-center gap-2 rounded-md border border-border bg-bg-elev px-4 py-2 hover:border-primary">
                        <button onClick={() => navigate(`/c/@${username}/chats/${c.id}`)}
                          className="flex-1 truncate text-left text-sm">
                          {c.title}
                        </button>
                        <button
                          onClick={() => { if (confirm('Delete this chat?')) delChat.mutate(c.id) }}
                          className="text-text-faint opacity-0 transition-opacity group-hover:opacity-100 hover:text-danger"
                          title="Delete chat">
                          <Trash2 size={15} strokeWidth={1.75} />
                        </button>
                      </div>
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
