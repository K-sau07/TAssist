import { useRef, useState } from 'react'
import { SendHorizontal, FileText } from 'lucide-react'
import { useMentionPicker } from './useMentionPicker'

export function Composer({ disabled, onSend }: { disabled?: boolean; onSend: (text: string) => void }) {
  const [text, setText] = useState('')
  const ref = useRef<HTMLTextAreaElement>(null)
  const picker = useMentionPicker(text)

  function grow() {
    const el = ref.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }
  function syncCaret() {
    if (ref.current) picker.setCaret(ref.current.selectionStart ?? 0)
  }
  function choose(index: number) {
    const file = picker.matches[index]
    if (!file) return
    const { text: next, caret } = picker.pick(file)
    setText(next)
    picker.reset()
    // restore caret after the inserted mention
    requestAnimationFrame(() => {
      const el = ref.current
      if (el) { el.focus(); el.setSelectionRange(caret, caret); picker.setCaret(caret) }
      grow()
    })
  }
  function submit() {
    const t = text.trim()
    if (!t || disabled) return
    onSend(t)
    setText('')
    picker.reset()
    if (ref.current) ref.current.style.height = 'auto'
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (picker.open) {
      if (e.key === 'ArrowDown') { e.preventDefault(); picker.setActive((picker.active + 1) % picker.matches.length); return }
      if (e.key === 'ArrowUp') { e.preventDefault(); picker.setActive((picker.active - 1 + picker.matches.length) % picker.matches.length); return }
      if (e.key === 'Enter' || e.key === 'Tab') { e.preventDefault(); choose(picker.active); return }
      if (e.key === 'Escape') { e.preventDefault(); picker.reset(); return }
    }
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit() }
  }

  return (
    <div className="border-t border-border bg-bg px-6 py-4">
      <div className="relative mx-auto max-w-3xl">
        {/* mention dropdown */}
        {picker.open && (
          <div className="absolute bottom-full mb-2 w-72 overflow-hidden rounded-lg border border-border bg-bg-elev shadow-2">
            {picker.matches.map((f, i) => (
              <button
                key={f.id}
                onMouseDown={(e) => { e.preventDefault(); choose(i) }}
                onMouseEnter={() => picker.setActive(i)}
                className={`flex w-full items-center gap-2 px-3 py-2 text-left text-sm ${i === picker.active ? 'bg-primary/10 text-primary' : 'hover:bg-bg-sunken'}`}
              >
                <FileText size={15} strokeWidth={1.75} className="shrink-0 text-text-faint" />
                <span className="truncate">{f.originalFilename}</span>
              </button>
            ))}
          </div>
        )}

        <div className="flex items-end gap-2 rounded-lg border border-border bg-bg-elev p-2 focus-within:border-primary">
          <textarea
            ref={ref}
            value={text}
            rows={1}
            placeholder="Ask a question…  (@ to mention a file)"
            className="max-h-[200px] flex-1 resize-none bg-transparent px-2 py-1.5 text-md outline-none placeholder:text-text-faint"
            onChange={(e) => { setText(e.target.value); grow(); syncCaret() }}
            onKeyUp={syncCaret}
            onClick={syncCaret}
            onKeyDown={onKeyDown}
          />
          <button
            onClick={submit}
            disabled={disabled || !text.trim()}
            className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-primary text-primary-fg disabled:opacity-40"
          >
            <SendHorizontal size={18} strokeWidth={1.75} />
          </button>
        </div>
      </div>
    </div>
  )
}
