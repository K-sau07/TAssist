import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

/** Placeholder page shell used during scaffolding (Step 15). Real pages land in later steps. */
export function Stub({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <p className="text-xs uppercase tracking-widest text-text-faint">TAssist</p>
      <h1 className="mt-2 text-3xl">{title}</h1>
      <p className="mt-4 text-text-muted">This page is scaffolded. Real UI arrives in a later step.</p>
      {children}
      <p className="mt-8 text-sm">
        <Link to="/app" className="text-primary hover:underline">→ Dashboard</Link>
        <span className="mx-2 text-text-faint">·</span>
        <Link to="/" className="text-primary hover:underline">→ Landing</Link>
      </p>
    </main>
  )
}
