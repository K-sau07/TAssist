import { Fragment } from 'react'

const CITE = /\[S(\d+)\]/g

/** Renders text with [Sn] markers turned into clickable citation chips. */
export function MessageContent({ text, onCite }: { text: string; onCite?: (num: number) => void }) {
  const parts: Array<string | number> = []
  let last = 0
  let m: RegExpExecArray | null
  CITE.lastIndex = 0
  while ((m = CITE.exec(text)) !== null) {
    if (m.index > last) parts.push(text.slice(last, m.index))
    parts.push(Number(m[1]))
    last = m.index + m[0].length
  }
  if (last < text.length) parts.push(text.slice(last))

  return (
    <span className="whitespace-pre-wrap leading-relaxed">
      {parts.map((p, i) =>
        typeof p === 'number' ? (
          <button
            key={i}
            onClick={() => onCite?.(p)}
            className="mx-0.5 inline-flex h-5 items-center rounded-round bg-primary/12 px-1.5 align-middle text-xs font-medium text-primary hover:bg-primary/20"
          >
            S{p}
          </button>
        ) : (
          <Fragment key={i}>{p}</Fragment>
        ),
      )}
    </span>
  )
}
