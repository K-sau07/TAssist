import { useState } from 'react'
import { useChannelContext } from '@/features/channels/shell/ChannelShell'
import { OverviewTab } from './tabs/OverviewTab'
import { FilesTab } from './tabs/FilesTab'
import { MembersTab } from './tabs/MembersTab'

type Tab = 'overview' | 'files' | 'members'
const TABS: Tab[] = ['overview', 'files', 'members']

/**
 * Owner-only channel admin (03_CHANNEL_SHELL_SPEC §5). Renders inside the shell —
 * no AppLayout, channel from context. Messaging is NOT a tab here (D-CS5); it lives
 * in the channel rail like every other participant.
 */
export default function ChannelManagePage() {
  const { channel } = useChannelContext()
  const [tab, setTab] = useState<Tab>('overview')

  return (
    <div className="h-screen overflow-y-auto">
      <main className="mx-auto max-w-4xl px-8 py-8">
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
        {tab === 'files' && <FilesTab channelId={channel.id} />}
        {tab === 'members' && <MembersTab channelId={channel.id} />}
      </main>
    </div>
  )
}
