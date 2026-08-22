import type { FileStatus } from '@/lib/api/files'
import { cn } from '@/lib/cn'

const LABEL: Record<FileStatus, string> = {
  UPLOADING: 'Uploading', PARSING: 'Parsing', EMBEDDING: 'Embedding',
  READY: 'Ready', FAILED: 'Failed',
}
const STYLE: Record<FileStatus, string> = {
  UPLOADING: 'bg-accent-peach/25 text-text',
  PARSING:   'bg-accent-peach/25 text-text',
  EMBEDDING: 'bg-accent-amber/25 text-text',
  READY:     'bg-accent-mint/40 text-text',
  FAILED:    'bg-danger/15 text-danger',
}
const inProgress = (s: FileStatus) => s === 'UPLOADING' || s === 'PARSING' || s === 'EMBEDDING'

export function StatusPill({ status }: { status: FileStatus }) {
  return (
    <span className={cn('inline-flex items-center gap-1.5 rounded-round px-2.5 py-0.5 text-xs font-medium', STYLE[status])}>
      {inProgress(status) && <span className="h-1.5 w-1.5 animate-pulse rounded-round bg-current" />}
      {LABEL[status]}
    </span>
  )
}
