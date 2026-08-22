import { useParams, useNavigate } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { FileGrid } from '@/features/files/FileGrid'
import { useFolderFilesQuery, useFoldersQuery, useDeleteFolderMutation } from '@/lib/hooks/useFolders'
import { Button } from '@/design/components/Button'
import { Trash2 } from 'lucide-react'

export default function FolderPage() {
  const { folderId = '' } = useParams()
  const navigate = useNavigate()
  const { data: folders = [] } = useFoldersQuery()
  const { data: files = [], isLoading } = useFolderFilesQuery(folderId)
  const del = useDeleteFolderMutation()
  const folder = folders.find((f) => f.id === folderId)

  return (
    <AppLayout>
      <main className="mx-auto max-w-6xl px-8 py-8">
        <header className="mb-6 flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-widest text-text-faint">Folder</p>
            <h1 className="mt-1 text-3xl">{folder?.name ?? 'Folder'}</h1>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="secondary" onClick={() => navigate('/app/chats/new')}>Start chat in this folder</Button>
            <Button variant="ghost" onClick={() => {
              if (confirm('Delete this folder? Files are kept in your library.')) {
                del.mutate(folderId, { onSuccess: () => navigate('/app') })
              }
            }}><Trash2 size={16} strokeWidth={1.75} /></Button>
          </div>
        </header>
        {isLoading ? <p className="text-text-muted">Loading…</p> : <FileGrid files={files} />}
      </main>
    </AppLayout>
  )
}
