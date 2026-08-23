import { useState } from 'react'
import { Trash2, Plus } from 'lucide-react'
import { Button } from '@/design/components/Button'
import { Input } from '@/design/components/Input'
import { useChannelFilesQuery, useAttachFileMutation, useDetachFileMutation } from '@/lib/hooks/useChannels'
import { useFilesQuery } from '@/lib/hooks/useFiles'
import { useDialog } from '@/design/components/Dialog'

export function FilesTab({ channelId }: { channelId: string }) {
  const { data: attached = [] } = useChannelFilesQuery(channelId)
  const { data: library = [] } = useFilesQuery()
  const attach = useAttachFileMutation(channelId)
  const detach = useDetachFileMutation(channelId)
  const dialog = useDialog()
  const [picking, setPicking] = useState(false)

  const attachedIds = new Set(attached.map((a) => a.fileId))
  const available = library.filter((f) => f.status === 'READY' && !attachedIds.has(f.id))

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg">Attached files</h2>
        <Button variant="secondary" onClick={() => setPicking((v) => !v)}>
          <Plus size={16} strokeWidth={1.75} /> Add file
        </Button>
      </div>

      {attached.length === 0 && <p className="text-sm text-text-muted">No files attached yet.</p>}
      <div className="space-y-2">
        {attached.map((f) => (
          <div key={f.fileId} className="flex items-center justify-between rounded-md border border-border bg-bg-elev px-4 py-3">
            <div>
              <p className="text-sm font-medium">{f.displayLabel}</p>
              <p className="text-xs text-text-faint">Visitors see this label — never the real filename.</p>
            </div>
            <button className="text-text-faint hover:text-danger"
              onClick={async () => { if (await dialog.confirm({ title: 'Remove file from channel?', message: 'Chats in this channel that cited it will be deleted.', confirmLabel: 'Remove', danger: true })) detach.mutate(f.fileId) }}>
              <Trash2 size={16} strokeWidth={1.75} />
            </button>
          </div>
        ))}
      </div>

      {picking && (
        <div className="mt-6 rounded-lg border border-border bg-bg-sunken p-4">
          <p className="mb-3 text-sm font-medium">Attach from your library</p>
          {available.length === 0 && <p className="text-sm text-text-muted">No READY files available to attach.</p>}
          <div className="space-y-2">
            {available.map((f) => <AttachRow key={f.id} name={f.originalFilename}
              onAttach={(label) => attach.mutate({ fileId: f.id, displayLabel: label })} />)}
          </div>
        </div>
      )}
    </div>
  )
}

function AttachRow({ name, onAttach }: { name: string; onAttach: (label: string) => void }) {
  const [label, setLabel] = useState(name)
  return (
    <div className="flex items-center gap-2 rounded-md bg-bg-elev p-2">
      <span className="w-40 truncate text-sm text-text-muted" title={name}>{name}</span>
      <Input value={label} onChange={(e) => setLabel(e.target.value)} placeholder="Display label" className="h-9 flex-1" />
      <Button size="sm" onClick={() => onAttach(label.trim() || name)}>Attach</Button>
    </div>
  )
}
