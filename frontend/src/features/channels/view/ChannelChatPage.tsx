import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageList } from '@/features/chat/components/MessageList'
import { Composer } from '@/features/chat/components/Composer'
import { SnippetDrawer } from '@/features/chat/components/SnippetDrawer'
import { useChannelChatStream } from './useChannelChatStream'
import { useChannelContext } from '@/features/channels/shell/ChannelShell'
import { getChannelChat, createChannelChat } from '@/lib/api/channelChat'
import type { SourceItem } from '@/lib/sse/types'
import type { MessageView } from '@/lib/api/chats'

export default function ChannelChatPage() {
  const { chatId = '' } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { channel, username } = useChannelContext()
  const channelId = channel.id
  const isNew = chatId === 'new'
  const [creating, setCreating] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['channel-chat', chatId],
    queryFn: () => getChannelChat(channelId, chatId),
    enabled: Boolean(channelId && chatId) && !isNew, // don't fetch a chat that doesn't exist yet
  })
  // stream hook targets whatever chatId is in the URL (real id once created)
  const { state, send } = useChannelChatStream(channelId, chatId)
  const [drawer, setDrawer] = useState<SourceItem | null>(null)

  // For a brand-new chat: create it on first send, then stream against the real id.
  async function handleSend(text: string) {
    if (!channelId) return
    if (isNew) {
      setCreating(true)
      try {
        const chat = await createChannelChat(channelId)
        qc.invalidateQueries({ queryKey: ['channel', channelId, 'my-chats'] })
        // navigate to the real chat, then stream the first message there
        navigate(`/c/@${username}/chat/${chat.id}`, { replace: true })
        // hand off the message to the new page via sessionStorage (survives the nav)
        sessionStorage.setItem(`pending-msg:${chat.id}`, text)
      } finally { setCreating(false) }
      return
    }
    send(text)
  }

  const messages: MessageView[] = (data?.messages ?? []).map((m) => ({
    id: m.id, role: m.role, content: m.content, citations: [], mentionedFiles: [], createdAt: m.createdAt,
  }))

  // On arrival from the new-chat flow, send the handed-off first message exactly once.
  const sentPending = useRef(false)
  useEffect(() => {
    if (isNew || !chatId || sentPending.current) return
    const pending = sessionStorage.getItem(`pending-msg:${chatId}`)
    if (pending) {
      sentPending.current = true
      sessionStorage.removeItem(`pending-msg:${chatId}`)
      send(pending)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isNew, chatId])


  return (
    <>
      <div className="flex h-screen flex-col">
        <header className="flex items-center gap-3 border-b border-border bg-bg-elev px-6 py-4">
          <h1 className="text-lg">{channel.displayName}</h1>
          <span className="rounded-round bg-bg-sunken px-2 py-0.5 text-xs text-text-muted">@{username}</span>
        </header>
        {isLoading && !isNew
          ? <div className="flex-1 grid place-items-center text-text-muted">Loading…</div>
          : messages.length === 0 && !state.active
            ? <div className="flex-1 grid place-items-center px-6 text-center text-text-muted">
                <div>
                  <p className="text-lg">Ask {channel.displayName} a question</p>
                  <p className="mt-1 text-sm">Answers are grounded in the channel's documents.</p>
                </div>
              </div>
            : <MessageList messages={messages} stream={state} onCite={setDrawer} />}
        <Composer disabled={state.active || creating} onSend={handleSend} />
      </div>
      <SnippetDrawer source={drawer} onClose={() => setDrawer(null)} />
    </>
  )
}
