import { useRef, useState } from 'react'
import { SendHorizontal } from 'lucide-react'

export function Composer({ disabled, onSend }: { disabled?: boolean; onSend: (text: string) => void }) {
  const [text, setText] = useState('')
  const ref = useRef<HTMLTextAreaElement>(null)

  function grow() {
    const el = ref.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }
  function submit() {
    const t = text.trim()
    if (!t || disabled) return
    onSend(t)
    setText('')
    if (ref.current) ref.current.style.height = 'auto'
  }

  return (
    <div className="border-t border-border bg-bg px-6 py-4">
      <div className="mx-auto flex max-w-3xl items-end gap-2 rounded-lg border border-border bg-bg-elev p-2 focus-within:border-primary">
        <textarea
          ref={ref}
          value={text}
          rows={1}
          placeholder="Ask a question…  (@ to mention a file)"
          className="max-h-[200px] flex-1 resize-none bg-transparent px-2 py-1.5 text-md outline-none placeholder:text-text-faint"
          onChange={(e) => { setText(e.target.value); grow() }}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit() }
          }}
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
  )
}
