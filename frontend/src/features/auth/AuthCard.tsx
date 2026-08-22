import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { googleAuthorizeUrl } from '@/lib/api/auth'
import { Button } from '@/design/components/Button'

export function AuthCard({
  title, subtitle, children, footer,
}: { title: string; subtitle?: string; children: ReactNode; footer: ReactNode }) {
  return (
    <main className="min-h-screen grid place-items-center bg-bg px-4">
      <div className="w-full max-w-[420px]">
        <Link to="/" className="mb-6 block text-center font-display text-2xl text-primary">TAssist</Link>
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
        <p className="mt-4 text-center text-sm text-text-muted">{footer}</p>
      </div>
    </main>
  )
}

export function FieldError({ msg }: { msg?: string }) {
  if (!msg) return null
  return <p className="mt-1 text-xs text-danger">{msg}</p>
}
