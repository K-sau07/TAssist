import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Sparkles, BookOpen } from 'lucide-react'
import { googleAuthorizeUrl } from '@/lib/api/auth'
import { Button } from '@/design/components/Button'

export function AuthCard({
  title, subtitle, children, footer,
}: { title: string; subtitle?: string; children: ReactNode; footer: ReactNode }) {
  return (
    <main className="grid min-h-screen bg-bg lg:grid-cols-2">
      {/* form side */}
      <div className="grid place-items-center px-4 py-10">
        <div className="w-full max-w-[420px]">
          <Link to="/" className="mb-6 block text-center font-display text-2xl text-primary lg:text-left">TAssist</Link>
          <div className="rounded-lg border border-border bg-bg-elev p-8 shadow-1">
            <h1 className="text-2xl">{title}</h1>
            {subtitle && <p className="mt-1 text-sm text-text-muted">{subtitle}</p>}
            <div className="mt-6">{children}</div>

            <div className="my-5 flex items-center gap-3 text-xs text-text-faint">
              <span className="h-px flex-1 bg-border" /> or <span className="h-px flex-1 bg-border" />
            </div>

            <a href={googleAuthorizeUrl} className="block">
              <Button type="button" variant="secondary" className="w-full">Continue with Google</Button>
            </a>
          </div>
          <p className="mt-4 text-center text-sm text-text-muted lg:text-left">{footer}</p>
        </div>
      </div>

      {/* brand side (hidden on small screens) */}
      <aside className="relative hidden overflow-hidden border-l border-border bg-bg-sunken lg:block">
        <div className="flex h-full flex-col justify-center px-12 py-16">
          <p className="text-2xs font-semibold uppercase tracking-[0.2em] text-text-faint">TAssist</p>
          <h2 className="mt-3 max-w-md font-display text-3xl leading-tight text-text">
            A calm place to study, where every answer shows its work.
          </h2>
          <p className="mt-3 max-w-md text-sm text-text-muted">
            Ask questions grounded only in your course documents — with citations you can trace, never a hallucination.
          </p>

          {/* a static preview of the AI margin-note signature */}
          <div className="mt-8 max-w-md rounded-lg border-l-[3px] border-l-[var(--primary-rule)] bg-bg-elev p-4 shadow-1">
            <span className="inline-flex items-center gap-1 text-2xs font-semibold uppercase tracking-wider text-primary">
              <Sparkles size={12} /> AI · grounded in 2 sources
            </span>
            <p className="mt-1.5 text-md leading-[1.6] text-text">
              Backpropagation computes gradients by applying the chain rule layer by layer, from the output back toward the input.
            </p>
            <div className="mt-3 flex flex-wrap gap-1.5 border-t border-border/60 pt-2">
              <span className="inline-flex items-center gap-1.5 rounded-md border border-border bg-bg-elev px-2 py-1 text-xs">
                <sup className="font-mono text-[10px] font-semibold text-primary">1</sup> Lecture 07 — Neural Nets
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-md border border-border bg-bg-elev px-2 py-1 text-xs">
                <sup className="font-mono text-[10px] font-semibold text-primary">2</sup> Backprop notes
              </span>
            </div>
          </div>

          <p className="mt-8 flex items-center gap-2 text-xs text-text-faint">
            <BookOpen size={13} /> Answers cite the exact lecture and slide they came from.
          </p>
        </div>
      </aside>
    </main>
  )
}

export function FieldError({ msg }: { msg?: string }) {
  if (!msg) return null
  return <p className="mt-1 text-xs text-danger">{msg}</p>
}
