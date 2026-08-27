import { useEffect, useState } from 'react'
import { useReducedMotion } from 'framer-motion'
import { Sparkles } from 'lucide-react'

/**
 * Hero demo (05_GLOWUP §E): the product doing real work — a student's question
 * resolves into the AI "margin note" with footnote citations that write on.
 * Self-playing loop; reduced-motion shows the finished state immediately (§A5).
 */
const ANSWER = 'Backpropagation computes gradients with the chain rule, propagating error backward from the output layer to each weight.'

export function MarginNoteDemo() {
  const reduce = useReducedMotion()
  const [phase, setPhase] = useState(reduce ? 3 : 0) // 0 ask, 1 thinking, 2 answering, 3 done
  const [typed, setTyped] = useState(reduce ? ANSWER : '')

  useEffect(() => {
    if (reduce) return
    let t: ReturnType<typeof setTimeout>
    const run = () => {
      setPhase(0); setTyped('')
      t = setTimeout(() => setPhase(1), 1100)          // show question, then think
      t = setTimeout(() => { setPhase(2); type(0) }, 2100)
    }
    const type = (i: number) => {
      if (i > ANSWER.length) { setPhase(3); t = setTimeout(run, 4200); return }
      setTyped(ANSWER.slice(0, i))
      t = setTimeout(() => type(i + 2), 24)
    }
    run()
    return () => clearTimeout(t)
  }, [reduce])

  return (
    <div className="mx-auto w-full max-w-md rounded-xl border border-border bg-bg-elev p-5 shadow-2">
      {/* the question */}
      <div className="mb-4 flex items-start gap-2.5">
        <span className="grid h-7 w-7 shrink-0 place-items-center rounded-round text-xs font-semibold text-white"
          style={{ background: 'var(--avatar-3)' }}>SK</span>
        <div className="rounded-lg rounded-tl-sm bg-bg-sunken px-3 py-2 text-sm text-text">
          <span className="font-mono text-primary">@ai</span> how does backpropagation work?
        </div>
      </div>

      {/* the margin note */}
      <div className="rounded-lg border-l-[3px] border-l-[var(--primary-rule)] bg-primary-wash px-4 py-3 shadow-1">
        <span className="inline-flex items-center gap-1 text-2xs font-semibold uppercase tracking-wider text-primary">
          <Sparkles size={12} /> {phase < 2 ? 'AI · searching your documents…' : 'AI · grounded in 2 sources'}
        </span>

        {phase === 1 && (
          <div className="mt-2 flex gap-1" aria-hidden>
            {[0, 1, 2].map((i) => (
              <span key={i} className="h-1.5 w-1.5 animate-pulse rounded-round bg-primary/50"
                style={{ animationDelay: `${i * 150}ms` }} />
            ))}
          </div>
        )}

        {phase >= 2 && (
          <>
            <p className="mt-1.5 text-md leading-[1.6] text-text">
              {typed}{phase === 2 && <span className="ml-0.5 inline-block h-4 w-0.5 -translate-y-0.5 animate-pulse bg-primary align-middle" />}
            </p>
            {phase === 3 && (
              <div className="mt-3 flex flex-wrap gap-1.5 border-t border-border/60 pt-2 animate-msg-in">
                <Chip n={1} label="Lecture 07 — Neural Nets" />
                <Chip n={2} label="Backprop notes" />
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

function Chip({ n, label }: { n: number; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-md border border-border bg-bg-elev px-2 py-1 text-xs text-text">
      <sup className="font-mono text-[10px] font-semibold text-primary">{n}</sup>{label}
    </span>
  )
}
