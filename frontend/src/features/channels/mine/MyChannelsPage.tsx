import { Link, useNavigate } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Button } from '@/design/components/Button'
import { useMyChannelsQuery } from '@/lib/hooks/useChannels'
import { Hash, Plus } from 'lucide-react'

export default function MyChannelsPage() {
  const navigate = useNavigate()
  const { data: channels = [], isLoading } = useMyChannelsQuery()

  return (
    <AppLayout>
      <main className="mx-auto max-w-5xl px-8 py-8">
        <header className="mb-8 flex items-center justify-between">
          <h1 className="text-3xl">My channels</h1>
          <Button onClick={() => navigate('/app/channels/new')}>
            <Plus size={18} strokeWidth={1.75} /> Create channel
          </Button>
        </header>

        {isLoading ? (
          <p className="text-text-muted">Loading…</p>
        ) : channels.length === 0 ? (
          <div className="grid place-items-center rounded-lg border-2 border-dashed border-border-strong p-12 text-center">
            <p className="text-lg font-medium">You haven't created any channels yet</p>
            <p className="mt-1 text-sm text-text-muted">Publish a curated Q&A surface over some of your files.</p>
            <Button className="mt-4" onClick={() => navigate('/app/channels/new')}>Create channel</Button>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {channels.map((c) => (
              <Link key={c.id} to={`/c/@${c.username}`}
                className="flex items-start gap-3 rounded-lg border border-border bg-bg-elev p-5 shadow-1 transition-transform hover:-translate-y-0.5 hover:shadow-2">
                <div className="grid h-11 w-11 place-items-center rounded-round bg-bg-sunken text-primary">
                  <Hash size={20} strokeWidth={1.75} />
                </div>
                <div className="min-w-0">
                  <p className="truncate font-medium">{c.displayName}</p>
                  <p className="text-sm text-text-muted">@{c.username}</p>
                  <span className="mt-1 inline-block rounded-round bg-bg-sunken px-2 py-0.5 text-xs text-text-faint">
                    {c.visibility.toLowerCase()}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </AppLayout>
  )
}
