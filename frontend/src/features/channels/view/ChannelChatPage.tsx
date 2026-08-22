import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { MessageList } from '@/features/chat/components/MessageList'
import { Composer } from '@/features/chat/components/Composer'
import { SnippetDrawer } from '@/features/chat/components/SnippetDrawer'
import { useChannelChatStream } from './useChannelChatStream'
import { useChannelPublicQuery } from '@/lib/hooks/useDiscover'
import { getChannelChat } from '@/lib/api/channelChat'
import type { SourceItem } from '@/lib/sse/types'
import type { MessageView } from '@/lib/api/chats'

export default function ChannelChatPage() {
  const { username = '', chatId = '' } = useParams()
  const { data: pub } = useChannelPublicQuery(username)
  const channelId = pub?.channel.id ?? ''

  const { data, isLoading } = useQuery({
    queryKey: ['channel-chat', chatId],
    queryFn: () => getChannelChat(channelId, chatId),
    enabled: Boolean(channelId && chatId),
  })
  const { state, send } = useChannelChatStream(channelId, chatId)
  const [drawer, setDrawer] = useState<SourceItem | null>(null)

  // channel MsgView has no citations array; map to the shape MessageList expects
  const messages: MessageView[] = (data?.messages ?? []).map((m) => ({
    id: m.id, role: m.role, content: m.content, citations: [], mentionedFiles: [], createdAt: m.createdAt,
  }))

  return (
    <AppLayout>
      <div className="flex h-screen flex-col">
        <header className="flex items-center gap-3 border-b border-border bg-bg-elev px-6 py-4">
          <h1 className="text-lg">{pub?.channel.displayName ?? 'Channel'}</h1>
          <span className="rounded-round bg-bg-sunken px-2 py-0.5 text-xs text-text-muted">@{username}</span>
        </header>
        {isLoading
          ? <div className="flex-1 grid place-items-center text-text-muted">Loading…</div>
          : <MessageList messages={messages} stream={state} onCite={setDrawer} />}
        <Composer disabled={state.active} onSend={send} />
      </div>
      <SnippetDrawer source={drawer} onClose={() => setDrawer(null)} />
    </AppLayout>
  )
}
