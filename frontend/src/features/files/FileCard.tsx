import { FileText, Trash2 } from 'lucide-react'
import { StatusPill } from './StatusPill'
import { useFileStatusPoll } from './useFileStatusPoll'
import { fileSize, timeAgo } from '@/lib/format'
import { friendlyFailureReason } from '@/lib/ui/failureReason'
import type { FileView } from '@/lib/api/files'

export function FileCard({ file, onDelete }: { file: FileView; onDelete?: (id: string) => void }) {
  // live status via polling while in-progress; falls back to the file's own status
  const { data } = useFileStatusPoll(file.id, file.status, true)
  const status = data?.status ?? file.status
  const rawReason = data?.failureReason ?? file.failureReason

  return (
    <div className="group relative flex flex-col gap-3 rounded-lg border border-border bg-bg-elev p-4 shadow-1 transition-transform hover:-translate-y-0.5 hover:shadow-2">
      <div className="flex items-start justify-between">
        <div className="grid h-10 w-10 place-items-center rounded-md bg-bg-sunken text-primary">
          <FileText size={20} strokeWidth={1.75} />
        </div>
        <StatusPill status={status} />
      </div>
      <div>
        <p className="truncate text-sm font-medium" title={file.originalFilename}>{file.originalFilename}</p>
        <p className="mt-0.5 text-xs text-text-faint">
          {file.type} · {fileSize(file.sizeBytes)} · {timeAgo(file.createdAt)}
        </p>
        {status === 'FAILED' && (
          <p className="mt-1 text-xs text-danger" title={rawReason ?? undefined}>
            {friendlyFailureReason(rawReason)}
          </p>
        )}
      </div>
      {onDelete && (
        <button
          onClick={() => onDelete(file.id)}
          className="absolute right-3 top-3 opacity-0 transition-opacity group-hover:opacity-100 text-text-faint hover:text-danger"
          title="Delete file"
        >
          <Trash2 size={16} strokeWidth={1.75} />
        </button>
      )}
    </div>
  )
}
