import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Input } from '@/design/components/Input'
import { useDirectoryQuery } from '@/lib/hooks/useDiscover'
import { searchChannels, type ChannelView } from '@/lib/api/channels'
import { Hash, Search } from 'lucide-react'

export default function DiscoverChannelsPage() {
  const { data: directory = [], isLoading } = useDirectoryQuery()
  const [q, setQ] = useState('')
  const [results, setResults] = useState<ChannelView[] | null>(null)

  async function runSearch(term: string) {
    setQ(term)
    if (term.trim().length < 2) { setResults(null); return }
    try { setResults(await searchChannels(term.trim())) } catch { setResults([]) }
  }

  const shown = results ?? directory

  return (
    <AppLayout>
      <main className="mx-auto max-w-5xl px-8 py-8">
        <h1 className="text-3xl">Discover channels</h1>
        <p className="mt-1 text-text-muted">Find public Q&A channels and request access.</p>

        <div className="relative mt-6 max-w-md">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-faint" />
          <Input className="pl-9" placeholder="Search channels…" value={q} onChange={(e) => runSearch(e.target.value)} />
        </div>

        <div className="mt-8">
          {isLoading && !results ? <p className="text-text-muted">Loading…</p>
            : shown.length === 0 ? <p className="text-text-muted">No channels found.</p>
            : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {shown.map((c) => (
                  <Link key={c.id} to={`/c/@${c.username}`}
                    className="rounded-lg border border-border bg-bg-elev p-5 shadow-1 transition-transform hover:-translate-y-0.5 hover:shadow-2">
                    <div className="flex items-center gap-3">
                      <div className="grid h-10 w-10 place-items-center rounded-round bg-bg-sunken text-primary">
                        <Hash size={18} strokeWidth={1.75} />
                      </div>
                      <div className="min-w-0">
                        <p className="truncate font-medium">{c.displayName}</p>
                        <p className="text-sm text-text-muted">@{c.username}</p>
                      </div>
                    </div>
                    {c.description && <p className="mt-3 line-clamp-2 text-sm text-text-muted">{c.description}</p>}
                  </Link>
                ))}
              </div>
            )}
        </div>
      </main>
    </AppLayout>
  )
}
