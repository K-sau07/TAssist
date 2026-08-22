import { useEffect, useRef, useState } from 'react'
import { StickyNote } from 'lucide-react'
import { useNoteQuery, useUpdateNoteMutation } from '@/lib/hooks/useWidgets'

export function NotesCard() {
  const { data: note } = useNoteQuery()
  const save = useUpdateNoteMutation()
  const [value, setValue] = useState('')
  const [status, setStatus] = useState<'idle' | 'saving' | 'saved'>('idle')
  const loaded = useRef(false)
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)

  // hydrate once from server
  useEffect(() => {
    if (note && !loaded.current) { setValue(note.content); loaded.current = true }
  }, [note])

  function onChange(next: string) {
    setValue(next)
    setStatus('idle')
    clearTimeout(timer.current)
    timer.current = setTimeout(() => {
      setStatus('saving')
      save.mutate(next, { onSuccess: () => { setStatus('saved'); setTimeout(() => setStatus('idle'), 1500) } })
    }, 2000) // autosave 2s after idle
  }

  return (
    <div className="rounded-lg border border-border bg-bg-elev p-5 shadow-1">
      <div className="mb-2 flex items-center justify-between">
        <h3 className="flex items-center gap-2 font-medium"><StickyNote size={16} strokeWidth={1.75} /> Notes</h3>
        <span className="text-xs text-text-faint">
          {status === 'saving' ? 'Saving…' : status === 'saved' ? 'Saved' : ''}
        </span>
      </div>
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Jot something down…"
        className="h-40 w-full resize-none rounded-md bg-bg-sunken p-3 text-sm outline-none placeholder:text-text-faint focus:ring-2 focus:ring-primary/20"
      />
    </div>
  )
}
