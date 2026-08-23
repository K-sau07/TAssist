import { useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { FileGrid } from '@/features/files/FileGrid'
import {
  useFolderFilesQuery, useFoldersQuery, useDeleteFolderMutation, useAddFilesToFolderMutation,
  useUploadToFolderMutation,
} from '@/lib/hooks/useFolders'
import { useFilesQuery } from '@/lib/hooks/useFiles'
import { Button } from '@/design/components/Button'
import { useDialog } from '@/design/components/Dialog'
import { fileSize } from '@/lib/format'
import { Trash2, Plus, X, FileText, Check, Upload } from 'lucide-react'

export default function FolderPage() {
  const { folderId = '' } = useParams()
  const navigate = useNavigate()
  const { data: folders = [] } = useFoldersQuery()
  const { data: files = [], isLoading } = useFolderFilesQuery(folderId)
  const { data: library = [] } = useFilesQuery()
  const del = useDeleteFolderMutation()
  const addFiles = useAddFilesToFolderMutation(folderId)
  const uploadToFolder = useUploadToFolderMutation(folderId)
  const dialog = useDialog()
  const uploadInputRef = useRef<HTMLInputElement>(null)
  const folder = folders.find((f) => f.id === folderId)

  async function handleUpload(files: FileList | null) {
    if (!files) return
    for (const f of Array.from(files)) {
      try { await uploadToFolder.mutateAsync(f) } catch { /* surfaced via status pill / retry */ }
    }
    if (uploadInputRef.current) uploadInputRef.current.value = ''
  }

  const [picking, setPicking] = useState(false)
  const [selected, setSelected] = useState<Set<string>>(new Set())

  const inFolder = new Set(files.map((f) => f.id))
  const available = library.filter((f) => !inFolder.has(f.id))

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id); else next.add(id)
      return next
    })
  }
  function confirmAdd() {
    if (selected.size === 0) { setPicking(false); return }
    addFiles.mutate(Array.from(selected), {
      onSuccess: () => { setSelected(new Set()); setPicking(false) },
    })
  }

  return (
    <AppLayout>
      <main className="mx-auto max-w-6xl px-8 py-8">
        <header className="mb-6 flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-widest text-text-faint">Folder</p>
            <h1 className="mt-1 text-3xl">{folder?.name ?? 'Folder'}</h1>
          </div>
          <div className="flex items-center gap-2">
            <Button onClick={() => uploadInputRef.current?.click()} disabled={uploadToFolder.isPending}>
              <Upload size={16} strokeWidth={1.75} /> {uploadToFolder.isPending ? 'Uploading…' : 'Upload'}
            </Button>
            <Button variant="secondary" onClick={() => setPicking(true)}>
              <Plus size={16} strokeWidth={1.75} /> Add from library
            </Button>
            <Button variant="secondary" onClick={() => navigate('/app/chats/new')}>Start chat in this folder</Button>
            <Button variant="ghost" onClick={async () => {
              if (await dialog.confirm({ title: 'Delete folder?', message: 'The folder is removed but its files stay in your library.', confirmLabel: 'Delete', danger: true })) {
                del.mutate(folderId, { onSuccess: () => navigate('/app') })
              }
            }}><Trash2 size={16} strokeWidth={1.75} /></Button>
          </div>
        </header>

        {isLoading ? (
          <p className="text-text-muted">Loading…</p>
        ) : files.length === 0 ? (
          <div className="grid place-items-center rounded-lg border-2 border-dashed border-border-strong p-12 text-center">
            <p className="text-lg font-medium">This folder is empty</p>
            <p className="mt-1 text-sm text-text-muted">Upload a new file, or add one from your library.</p>
            <div className="mt-4 flex gap-2">
              <Button onClick={() => uploadInputRef.current?.click()} disabled={uploadToFolder.isPending}>
                <Upload size={16} strokeWidth={1.75} /> {uploadToFolder.isPending ? 'Uploading…' : 'Upload'}
              </Button>
              <Button variant="secondary" onClick={() => setPicking(true)}>
                <Plus size={16} strokeWidth={1.75} /> Add from library
              </Button>
            </div>
          </div>
        ) : (
          <FileGrid files={files} />
        )}
      </main>

      <input ref={uploadInputRef} type="file" multiple hidden
        accept=".pdf,.docx,.pptx,.txt,.md,.xlsx,.csv"
        onChange={(e) => handleUpload(e.target.files)} />

      {picking && (
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
        <div className="fixed inset-0 z-40 grid place-items-center bg-black/30 p-4"
             role="dialog" aria-modal="true" aria-label="Add files to folder"
             onClick={() => setPicking(false)} onKeyDown={(e) => { if (e.key === 'Escape') setPicking(false) }} tabIndex={-1}>
          {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions */}
          <div className="flex max-h-[80vh] w-full max-w-lg flex-col rounded-lg border border-border bg-bg-elev p-6 shadow-2"
               role="document" onClick={(e) => e.stopPropagation()}>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-medium">Add files from your library</h2>
              <button onClick={() => setPicking(false)} className="text-text-faint hover:text-text"><X size={18} /></button>
            </div>

            <div className="flex-1 space-y-1 overflow-y-auto">
              {available.length === 0 && (
                <p className="py-6 text-center text-sm text-text-muted">
                  Every library file is already in this folder. Upload more from the dashboard.
                </p>
              )}
              {available.map((f) => {
                const on = selected.has(f.id)
                return (
                  <button key={f.id} onClick={() => toggle(f.id)}
                    className={`flex w-full items-center gap-3 rounded-md border px-3 py-2 text-left ${on ? 'border-primary bg-primary/5' : 'border-border hover:border-border-strong'}`}>
                    <div className={`grid h-5 w-5 place-items-center rounded ${on ? 'bg-primary text-primary-fg' : 'border border-border'}`}>
                      {on && <Check size={13} strokeWidth={2.5} />}
                    </div>
                    <FileText size={16} strokeWidth={1.75} className="text-text-faint" />
                    <span className="flex-1 truncate text-sm">{f.originalFilename}</span>
                    <span className="text-xs text-text-faint">{fileSize(f.sizeBytes)}</span>
                  </button>
                )
              })}
            </div>

            <div className="mt-4 flex items-center justify-between">
              <span className="text-sm text-text-muted">{selected.size} selected</span>
              <div className="flex gap-2">
                <Button variant="ghost" onClick={() => setPicking(false)}>Cancel</Button>
                <Button onClick={confirmAdd} disabled={selected.size === 0 || addFiles.isPending}>
                  {addFiles.isPending ? 'Adding…' : `Add ${selected.size || ''}`.trim()}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  )
}
