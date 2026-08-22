import { useRef, useState } from 'react'
import { Upload, X } from 'lucide-react'
import { Button } from '@/design/components/Button'
import { useUploadFileMutation } from '@/lib/hooks/useFiles'
import { ApiError } from '@/lib/api/client'
import { cn } from '@/lib/cn'

export function UploadZone() {
  const [open, setOpen] = useState(false)
  const [dragOver, setDragOver] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const upload = useUploadFileMutation()

  async function handleFiles(files: FileList | null) {
    if (!files || files.length === 0) return
    setError(null)
    for (const f of Array.from(files)) {
      try { await upload.mutateAsync(f) }
      catch (e) { setError(e instanceof ApiError ? e.message : `Couldn't upload ${f.name}`) }
    }
    setOpen(false)
  }

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="fixed bottom-8 right-8 z-30 inline-flex items-center gap-2 rounded-round bg-primary px-5 py-3 text-primary-fg shadow-2 transition-transform hover:scale-[1.03] active:scale-95"
      >
        <Upload size={18} strokeWidth={1.75} /> Upload
      </button>

      {open && (
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
        <div className="fixed inset-0 z-40 grid place-items-center bg-black/30 p-4"
             role="dialog" aria-modal="true" aria-label="Upload files"
             onClick={() => setOpen(false)} onKeyDown={(e) => { if (e.key === 'Escape') setOpen(false) }} tabIndex={-1}>
          {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions */}
          <div className="w-full max-w-md rounded-lg border border-border bg-bg-elev p-6 shadow-2" role="document" onClick={(e) => e.stopPropagation()}>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-medium">Upload files</h2>
              <button onClick={() => setOpen(false)} className="text-text-faint hover:text-text"><X size={18} /></button>
            </div>
            <button
              type="button"
              onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
              onDragLeave={() => setDragOver(false)}
              onDrop={(e) => { e.preventDefault(); setDragOver(false); handleFiles(e.dataTransfer.files) }}
              onClick={() => inputRef.current?.click()}
              className={cn('grid w-full cursor-pointer place-items-center gap-2 rounded-md border-2 border-dashed p-10 text-center transition-colors',
                dragOver ? 'border-primary bg-primary/5' : 'border-border-strong hover:border-primary')}
            >
              <Upload size={28} strokeWidth={1.5} className="text-text-faint" />
              <p className="text-sm text-text-muted">Drop files here or click to browse</p>
              <p className="text-xs text-text-faint">PDF, DOCX, PPTX, TXT, MD, XLSX, CSV</p>
            </button>
            <input ref={inputRef} type="file" multiple hidden
              accept=".pdf,.docx,.pptx,.txt,.md,.xlsx,.csv"
              onChange={(e) => handleFiles(e.target.files)} />
            {upload.isPending && <p className="mt-3 text-sm text-text-muted">Uploading…</p>}
            {error && <p className="mt-3 text-sm text-danger">{error}</p>}
            <div className="mt-4 flex justify-end">
              <Button variant="ghost" onClick={() => setOpen(false)}>Done</Button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
