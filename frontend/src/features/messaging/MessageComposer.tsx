import { useMemo, useRef, useState } from 'react'
import { SendHorizontal, Sparkles, User, FileText } from 'lucide-react'
import { activeToken } from '@/features/chat/components/useMentionPicker'
import { composerSuggestions, type Suggestion } from './logic'

interface Props {
  disabled?: boolean
  participants: Array<{ displayName: string }>
  files: Array<{ originalFilename: string }>
  onSend: (text: string) => void
}

/** Thread composer: @-autocomplete over @ai/@assist, participants, and channel files. */
export function MessageComposer({ disabled, participants, files, onSend }: Props) {
  const [text, setText] = useState('')
  const [caret, setCaret] = useState(0)
  const [active, setActive] = useState(0)
  const ref = useRef<HTMLTextAreaElement>(null)

  const token = useMemo(() => activeToken(text, caret), [text, caret])
  const suggestions = useMemo<Suggestion[]>(
    () => (token ? composerSuggestions(token.query, participants, files) : []),
    [token, participants, files],
  )
  const open = token !== null && suggestions.length > 0

  function grow() {
    const el = ref.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }
  function syncCaret() {
    if (ref.current) setCaret(ref.current.selectionStart ?? 0)
  }
  function choose(index: number) {
    const s = suggestions[index]
    if (!s || !token) return
    const insert = s.insert + ' '
    const next = text.slice(0, token.start) + insert + text.slice(caret)
    const newCaret = token.start + insert.length
    setText(next)
    setActive(0)
    requestAnimationFrame(() => {
      const el = ref.current
      if (el) { el.focus(); el.setSelectionRange(newCaret, newCaret); setCaret(newCaret) }
      grow()
    })
  }
  function submit() {
    const t = text.trim()
    if (!t || disabled) return
    onSend(t)
    setText('')
    setActive(0)
    if (ref.current) ref.current.style.height = 'auto'
  }
  function onKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (open) {
      if (e.key === 'ArrowDown') { e.preventDefault(); setActive((active + 1) % suggestions.length); return }
      if (e.key === 'ArrowUp') { e.preventDefault(); setActive((active - 1 + suggestions.length) % suggestions.length); return }
      if (e.key === 'Enter' || e.key === 'Tab') { e.preventDefault(); choose(active); return }
      if (e.key === 'Escape') { e.preventDefault(); setActive(0); setCaret(-1); return }
    }
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit() }
  }

  const kindMeta = (k: Suggestion['kind']) =>
    k === 'ai' ? { icon: <Sparkles size={15} className="shrink-0 text-primary" />, tag: 'AI' }
      : k === 'participant' ? { icon: <User size={15} className="shrink-0 text-text-faint" />, tag: 'Person' }
        : { icon: <FileText size={15} className="shrink-0 text-text-faint" />, tag: 'File' }

  return (
    <div className="border-t border-border bg-bg px-6 pb-4 pt-3">
      <div className="relative mx-auto max-w-3xl">
        {open && (
          <div className="absolute bottom-full mb-2 w-80 overflow-hidden rounded-lg border border-border bg-bg-elev shadow-2"
            role="listbox" aria-label="Mention suggestions">
            {suggestions.map((s, i) => {
              const meta = kindMeta(s.kind)
              return (
                <button key={s.insert + i} role="option" aria-selected={i === active}
                  onMouseDown={(e) => { e.preventDefault(); choose(i) }}
                  onMouseEnter={() => setActive(i)}
                  className={`flex w-full items-center gap-2 px-3 py-2 text-left text-sm transition-colors ${i === active ? 'bg-primary-wash text-primary' : 'hover:bg-bg-sunken'}`}>
                  {meta.icon}
                  <span className="min-w-0 flex-1 truncate">{s.label}</span>
                  <span className="shrink-0 rounded-round bg-bg-sunken px-1.5 text-[10px] uppercase tracking-wide text-text-faint">{meta.tag}</span>
                </button>
              )
            })}
          </div>
        )}
        <div className="flex items-end gap-2 rounded-lg border border-border bg-bg-elev p-2 shadow-1 transition-colors focus-within:border-primary focus-within:ring-2 focus-within:ring-focus/40">
          <textarea ref={ref} value={text} rows={1}
            placeholder="Message…  (type @ai for a grounded answer)"
            className="max-h-[200px] flex-1 resize-none bg-transparent px-2 py-1.5 text-md outline-none placeholder:text-text-faint"
            onChange={(e) => { setText(e.target.value); grow(); syncCaret() }}
            onKeyUp={syncCaret} onClick={syncCaret} onKeyDown={onKeyDown} />
          <button onClick={submit} disabled={disabled || !text.trim()}
            aria-label="Send message"
            className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-primary text-primary-fg transition-transform hover:bg-primary-hover active:scale-90 disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus">
            <SendHorizontal size={18} strokeWidth={1.75} />
          </button>
        </div>
        <p className="mt-1 px-1 text-right text-2xs text-text-faint">
          <kbd className="font-sans">Enter</kbd> to send · <kbd className="font-sans">Shift</kbd>+<kbd className="font-sans">Enter</kbd> for a new line
        </p>
      </div>
    </div>
  )
}
