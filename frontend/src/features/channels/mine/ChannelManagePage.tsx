import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { useMyChannelsQuery } from '@/lib/hooks/useChannels'
import { OverviewTab } from './tabs/OverviewTab'
import { FilesTab } from './tabs/FilesTab'
import { MembersTab } from './tabs/MembersTab'

type Tab = 'overview' | 'files' | 'members'
const TABS: Tab[] = ['overview', 'files', 'members']

export default function ChannelManagePage() {
  const { channelId = '' } = useParams()
  const [tab, setTab] = useState<Tab>('overview')
  const { data: channels = [], isLoading } = useMyChannelsQuery()
  const channel = channels.find((c) => c.id === channelId)

  return (
    <AppLayout>
      <main className="mx-auto max-w-4xl px-8 py-8">
        {isLoading ? <p className="text-text-muted">Loading…</p> : !channel ? (
          <p className="text-text-muted">Channel not found.</p>
        ) : (
          <>
            <header className="mb-6">
              <h1 className="text-3xl">{channel.displayName}</h1>
              <p className="text-text-muted">@{channel.username}</p>
            </header>

            <div className="mb-6 flex gap-1 border-b border-border">
              {TABS.map((t) => (
                <button key={t} onClick={() => setTab(t)}
                  className={`px-4 py-2 text-sm capitalize ${tab === t ? 'border-b-2 border-primary font-medium text-primary' : 'text-text-muted hover:text-text'}`}>
                  {t}
                </button>
              ))}
            </div>

            {tab === 'overview' && <OverviewTab channel={channel} />}
            {tab === 'files' && <FilesTab channelId={channelId} />}
            {tab === 'members' && <MembersTab channelId={channelId} />}
          </>
        )}
      </main>
    </AppLayout>
  )
}
