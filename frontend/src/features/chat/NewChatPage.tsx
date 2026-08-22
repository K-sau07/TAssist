import { useNavigate } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { createChat } from '@/lib/api/chats'
import { useFoldersQuery } from '@/lib/hooks/useFolders'
import { MessageSquare, FolderOpen } from 'lucide-react'
import { useState } from 'react'

export default function NewChatPage() {
  const navigate = useNavigate()
  const { data: folders = [] } = useFoldersQuery()
  const [busy, setBusy] = useState(false)

  async function start(scope: 'REGULAR' | 'FOLDER', folderId?: string) {
    setBusy(true)
    try {
      const chat = await createChat(scope, folderId)
      navigate(`/app/chats/${chat.id}`)
    } finally { setBusy(false) }
  }

  return (
    <AppLayout>
      <main className="mx-auto max-w-3xl px-8 py-12">
        <h1 className="text-3xl">Start a new chat</h1>
        <p className="mt-1 text-text-muted">Ask across your whole library, or scope to one folder.</p>

        <button
          disabled={busy}
          onClick={() => start('REGULAR')}
          className="mt-8 flex w-full items-center gap-4 rounded-lg border border-border bg-bg-elev p-5 text-left shadow-1 hover:-translate-y-0.5 hover:shadow-2 transition-transform disabled:opacity-50"
        >
          <div className="grid h-11 w-11 place-items-center rounded-md bg-bg-sunken text-primary">
            <MessageSquare size={22} strokeWidth={1.75} />
          </div>
          <div>
            <p className="font-medium">Regular chat</p>
            <p className="text-sm text-text-muted">Mention files with @ to ground answers.</p>
          </div>
        </button>

        {folders.length > 0 && (
          <div className="mt-6">
            <p className="mb-2 flex items-center gap-1.5 text-sm text-text-muted">
              <FolderOpen size={15} strokeWidth={1.75} /> Or scope to a folder
            </p>
            <div className="grid gap-2">
              {folders.map((f) => (
                <button
                  key={f.id}
                  disabled={busy}
                  onClick={() => start('FOLDER', f.id)}
                  className="flex items-center gap-3 rounded-md border border-border bg-bg-elev px-4 py-3 text-left hover:border-primary disabled:opacity-50"
                >
                  <FolderOpen size={18} strokeWidth={1.75} className="text-text-faint" /> {f.name}
                </button>
              ))}
            </div>
          </div>
        )}
      </main>
    </AppLayout>
  )
}
