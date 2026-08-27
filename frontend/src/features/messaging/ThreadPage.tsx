import { useEffect, useMemo, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useDialog } from '@/design/components/Dialog'
import { useAuthStore } from '@/lib/auth/store'
import { useChannelContext } from '@/features/channels/shell/ChannelShell'
import { useChannelFilesQuery } from '@/lib/hooks/useChannels'
import {
  useMessagesQuery, useParticipantsQuery, usePostMessageMutation,
  useDeleteMessageMutation, useMarkReadMutation, useMyDmsQuery, useGroupQuery,
} from '@/lib/hooks/useMessaging'
import type { MessageView } from '@/lib/api/messaging'
import { conversationStreamPath } from '@/lib/api/messaging'
import { subscribeConversation } from '@/lib/sse/subscribeConversation'
import { mergeMessage, applyDeleted, groupsWith, startsNewDay, dayDividerLabel } from './logic'
import { MessageComposer } from './MessageComposer'
import { MessageRow } from './MessageRow'
import { AiMarginNote } from './AiMarginNote'
import { Users } from 'lucide-react'

export default function ThreadPage() {
  const { conversationId = '' } = useParams()
  const me = useAuthStore((s) => s.user)
  const { channel, isOwner } = useChannelContext()
  const channelId = channel.id

  const msgQuery = useMessagesQuery(channelId, conversationId, Boolean(channelId))
  const { data: participants = [] } = useParticipantsQuery(channelId, Boolean(channelId))
  const { data: channelFiles = [] } = useChannelFilesQuery(channelId)
  const dms = useMyDmsQuery(channelId)
  const group = useGroupQuery(channelId)
  const post = usePostMessageMutation(channelId, conversationId)
  const del = useDeleteMessageMutation(channelId, conversationId)
  const markRead = useMarkReadMutation(channelId, conversationId)
  const dialog = useDialog()

  // Header title: "# Group" for the group room, else the DM partner's name.
  const isGroup = group.data?.id === conversationId
  const dmName = dms.data?.find((d) => d.id === conversationId)?.otherParticipant?.displayName

  // Local message list = server snapshot + live SSE deltas.
  const [live, setLive] = useState<MessageView[]>([])
  useEffect(() => { if (msgQuery.data) setLive(msgQuery.data) }, [msgQuery.data])

  const fileSuggestions = useMemo(
    () => channelFiles.map((f) => ({ originalFilename: f.displayLabel })),
    [channelFiles],
  )

  const bottomRef = useRef<HTMLDivElement>(null)
  const scrollToBottom = () => bottomRef.current?.scrollIntoView({ behavior: 'smooth' })

  // Subscribe to realtime events for this conversation.
  useEffect(() => {
    if (!channelId || !conversationId) return
    const ac = new AbortController()
    subscribeConversation(
      conversationStreamPath(channelId, conversationId),
      {
        onMessage: (e) => {
          setLive((cur) => mergeMessage(cur, sseToMessage(e, participants, me)))
          requestAnimationFrame(scrollToBottom)
        },
        onDeleted: (e) => setLive((cur) => applyDeleted(cur, e.messageId)),
        // read receipts refresh inbox badges elsewhere; no per-message state in v1
        onRead: () => {},
      },
      ac.signal,
    )
    return () => ac.abort()
  }, [channelId, conversationId, participants, me])

  // Mark read on open + whenever new messages arrive.
  useEffect(() => {
    if (channelId && conversationId && live.length > 0) markRead.mutate(undefined)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [channelId, conversationId, live.length])

  useEffect(() => { requestAnimationFrame(scrollToBottom) }, [msgQuery.data])

  async function send(text: string) {
    try {
      const res = await post.mutateAsync(text)
      setLive((cur) => {
        let next = mergeMessage(cur, res.message)
        if (res.aiReply) next = mergeMessage(next, res.aiReply)
        return next
      })
      requestAnimationFrame(scrollToBottom)
    } catch { /* ApiError surfaces via mutation state below */ }
  }

  async function remove(id: string) {
    const ok = await dialog.confirm({
      title: 'Delete message?', message: 'This message will be removed for everyone.',
      confirmLabel: 'Delete', danger: true,
    })
    if (ok) del.mutate(id)
  }

  return (
    <div className="flex h-screen flex-col">
      <header className="flex items-center gap-3 border-b border-border px-6 py-3">
        {isGroup ? (
          <>
            <div className="grid h-8 w-8 place-items-center rounded-round bg-primary/12 text-primary">
              <Users size={16} strokeWidth={1.75} />
            </div>
            <div>
              <p className="text-sm font-medium"># Group</p>
              <p className="text-xs text-text-muted">Everyone in @{channel.username}</p>
            </div>
          </>
        ) : (
          <>
            <div className="grid h-8 w-8 place-items-center rounded-round bg-bg-sunken text-sm font-medium">
              {(dmName ?? '?').charAt(0).toUpperCase()}
            </div>
            <div>
              <p className="text-sm font-medium">{dmName ?? 'Conversation'}</p>
              <p className="text-xs text-text-muted">Direct message</p>
            </div>
          </>
        )}
      </header>

      <div className="flex-1 overflow-y-auto px-6 py-6">
        <div className="mx-auto max-w-3xl">
          {msgQuery.isLoading && <p className="text-text-muted">Loading messages…</p>}
          {!msgQuery.isLoading && live.length === 0 && (
            <div className="flex flex-col items-center gap-2 py-14 text-center">
              <div className="grid h-12 w-12 place-items-center rounded-round bg-primary-wash text-primary">
                <Users size={22} strokeWidth={1.6} />
              </div>
              <p className="font-display text-lg text-text">
                {isGroup ? '# Group' : dmName ?? 'Conversation'}
              </p>
              <p className="max-w-sm text-sm text-text-muted">
                {isGroup
                  ? `This is the very beginning of the group chat — everyone in @${channel.username} is here. Say hello, or ask @ai a question grounded in the channel's documents.`
                  : `This is the beginning of your conversation with ${dmName ?? 'this member'}. Messages are just between you two.`}
              </p>
            </div>
          )}
          {live.map((m, i) => {
            const prev = live[i - 1]
            const newDay = startsNewDay(prev, m)
            // a day break always resets grouping
            const grouped = !newDay && groupsWith(prev, m)
            return (
              <div key={m.id}>
                {newDay && (
                  <div className="my-3 flex items-center gap-3" role="separator">
                    <span className="h-px flex-1 bg-border" />
                    <span className="rounded-round border border-border bg-bg px-2.5 py-0.5 text-2xs font-medium text-text-muted">
                      {dayDividerLabel(m.createdAt)}
                    </span>
                    <span className="h-px flex-1 bg-border" />
                  </div>
                )}
                {m.senderKind === 'AI' ? (
                  <AiMarginNote msg={m}
                    canDelete={m.sender?.userId === me?.id || isOwner}
                    onDelete={() => remove(m.id)} />
                ) : (
                  <MessageRow msg={m} grouped={grouped}
                    displayName={m.sender?.userId === me?.id ? 'You' : undefined}
                    canDelete={m.sender?.userId === me?.id || isOwner}
                    onDelete={() => remove(m.id)} />
                )}
              </div>
            )
          })}
          <div ref={bottomRef} />
        </div>
      </div>

      {post.isError && (
        <div className="mx-auto max-w-3xl px-6 pb-1 text-xs text-danger">
          Couldn’t send that message. Please try again.
        </div>
      )}
      <MessageComposer disabled={post.isPending} participants={participants} files={fileSuggestions} onSend={send} />
    </div>
  )
}

/** Convert a raw SSE message event into a MessageView (resolving sender name from participants). */
function sseToMessage(
  e: { messageId: string; senderKind: 'HUMAN' | 'AI'; senderId: string | null; content: string; createdAt: string },
  participants: Array<{ userId: string; displayName: string }>,
  me: { id: string; displayName: string } | null,
): MessageView {
  const sender = e.senderId
    ? {
        userId: e.senderId,
        // My own echoed message isn't in `participants` (that list excludes me),
        // so resolve my name from the session first, then participants, then a last resort.
        displayName: e.senderId === me?.id
          ? me.displayName
          : participants.find((p) => p.userId === e.senderId)?.displayName ?? 'Member',
      }
    : null
  return {
    id: e.messageId,
    senderKind: e.senderKind,
    sender,
    content: e.content,
    citations: [], // citations arrive via the POST response / refetch, not the stream payload
    createdAt: e.createdAt,
    deleted: false,
  }
}
