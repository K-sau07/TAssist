import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { getChat } from '@/lib/api/chats'
import { chatStreamPath } from '@/lib/api/chats'
import { MessageList } from './components/MessageList'
import { Composer } from './components/Composer'
import { SnippetDrawer } from './components/SnippetDrawer'
import { useChatStream } from './useChatStream'
import type { SourceItem } from '@/lib/sse/types'

export default function ChatPage() {
  const { chatId = '' } = useParams()
  const { data, isLoading } = useQuery({ queryKey: ['chat', chatId], queryFn: () => getChat(chatId) })
  const { state, send } = useChatStream(chatStreamPath, chatId)
  const [drawer, setDrawer] = useState<SourceItem | null>(null)

  const scopeLabel = data?.chat.scope === 'FOLDER' ? 'Folder chat'
    : data?.chat.scope === 'CHANNEL' ? 'Channel chat' : 'Regular chat'

  return (
    <AppLayout>
      <div className="flex h-screen flex-col">
        <header className="flex items-center gap-3 border-b border-border bg-bg-elev px-6 py-4">
          <h1 className="text-lg">{data?.chat.title ?? 'Chat'}</h1>
          <span className="rounded-round bg-bg-sunken px-2 py-0.5 text-xs text-text-muted">{scopeLabel}</span>
        </header>

        {isLoading
          ? <div className="flex-1 grid place-items-center text-text-muted">Loading…</div>
          : <MessageList messages={data?.messages ?? []} stream={state} onCite={setDrawer} />}

        <Composer disabled={state.active} onSend={send} />
      </div>
      <SnippetDrawer source={drawer} onClose={() => setDrawer(null)} />
    </AppLayout>
  )
}
