import { useDialog } from '@/design/components/Dialog'
import { FileCard } from './FileCard'
import type { FileView } from '@/lib/api/files'
import { useDeleteFileMutation } from '@/lib/hooks/useFiles'

export function FileGrid({ files }: { files: FileView[] }) {
  const del = useDeleteFileMutation()
  const dialog = useDialog()
  if (files.length === 0) {
    return (
      <div className="grid place-items-center rounded-lg border-2 border-dashed border-border-strong p-12 text-center">
        <p className="text-lg font-medium">Drop your first file</p>
        <p className="mt-1 text-sm text-text-muted">Upload a document to start asking questions grounded in it.</p>
      </div>
    )
  }
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
      {files.map((f) => (
        <FileCard key={f.id} file={f}
          onDelete={async (id) => {
            if (await dialog.confirm({ title: 'Delete file?', message: 'This removes the file and its chunks. This cannot be undone.', confirmLabel: 'Delete', danger: true }))
              del.mutate(id)
          }} />
      ))}
    </div>
  )
}
