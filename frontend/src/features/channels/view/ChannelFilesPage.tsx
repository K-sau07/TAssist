import { useChannelContext } from '@/features/channels/shell/ChannelShell'
import { useChannelFilesQuery } from '@/lib/hooks/useChannels'
import { timeAgo } from '@/lib/format'
import { FileText } from 'lucide-react'

/**
 * Member-facing read-only view of the channel's documents (03_CHANNEL_SHELL_SPEC §5, D-CS4).
 * Shows only the owner's display_label per §7.5 — never raw filenames. Owners manage the
 * actual file set via the Manage → Files tab; this is the "what's in here" surface for everyone.
 */
export default function ChannelFilesPage() {
  const { channel } = useChannelContext()
  const { data: files = [], isLoading } = useChannelFilesQuery(channel.id)

  return (
    <div className="h-screen overflow-y-auto">
      <header className="border-b border-border px-6 py-4">
        <h1 className="text-lg">Files</h1>
        <p className="text-xs text-text-muted">Documents the AI answers from in @{channel.username}</p>
      </header>

      <div className="mx-auto max-w-2xl px-6 py-8">
        {isLoading && <p className="text-text-muted">Loading…</p>}
        {!isLoading && files.length === 0 && (
          <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-text-muted">
            <FileText size={22} className="mx-auto mb-2 text-text-faint" />
            No documents have been added to this channel yet.
          </div>
        )}
        <div className="space-y-2">
          {files.map((f) => (
            <div key={f.fileId}
              className="flex items-center gap-3 rounded-lg border border-border bg-bg-elev px-4 py-3">
              <div className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-primary-wash text-primary">
                <FileText size={17} strokeWidth={1.75} />
              </div>
              <span className="flex-1 truncate text-sm">{f.displayLabel}</span>
              <span className="shrink-0 text-xs text-text-faint">{timeAgo(f.addedAt)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
