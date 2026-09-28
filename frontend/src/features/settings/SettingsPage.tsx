import { useNavigate } from 'react-router-dom'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Button } from '@/design/components/Button'
import { useDialog } from '@/design/components/Dialog'
import { useAuthStore } from '@/lib/auth/store'
import { useQuotaQuery } from '@/lib/hooks/useQuota'
import { logout as logoutRequest } from '@/lib/api/auth'
import { fileSize } from '@/lib/format'
import { LogOut, User, Gauge, Info } from 'lucide-react'

/** §16.2 — account details and current-period usage against quota limits. */
export default function SettingsPage() {
  const navigate = useNavigate()
  const dialog = useDialog()
  const user = useAuthStore((s) => s.user)
  const clear = useAuthStore((s) => s.clear)
  const { data: quota, isLoading, isError } = useQuotaQuery()

  async function handleSignOut() {
    const ok = await dialog.confirm({
      title: 'Sign out?',
      message: 'You will need to sign in again to reach your library.',
      confirmLabel: 'Sign out',
    })
    if (!ok) return
    try { await logoutRequest() } catch { /* clearing locally is what matters */ }
    clear()
    navigate('/login', { replace: true })
  }

  return (
    <AppLayout>
      <main className="mx-auto max-w-3xl px-6 py-12">
        <h1 className="text-3xl">Settings</h1>
        <p className="mt-2 text-text-muted">Your account and current usage.</p>

        {/* ── account ─────────────────────────────────────── */}
        <section className="mt-12">
          <h2 className="flex items-center gap-2 text-sm uppercase tracking-widest text-text-faint">
            <User size={14} strokeWidth={1.75} /> Account
          </h2>
          <dl className="mt-4 divide-y divide-border rounded-lg border border-border">
            <Row label="Display name" value={user?.displayName ?? '—'} />
            <Row label="Email" value={user?.email ?? '—'} />
          </dl>
          <p className="mt-3 flex items-start gap-2 text-sm text-text-faint">
            <Info size={14} strokeWidth={1.75} className="mt-1 shrink-0" />
            Profile editing isn&apos;t available yet — the API has no update endpoint.
          </p>
        </section>

        {/* ── usage ───────────────────────────────────────── */}
        <section className="mt-12">
          <h2 className="flex items-center gap-2 text-sm uppercase tracking-widest text-text-faint">
            <Gauge size={14} strokeWidth={1.75} /> Usage
            {quota && <span className="normal-case tracking-normal">· {quota.period}</span>}
          </h2>

          {isLoading && <p className="mt-4 text-text-muted">Loading usage…</p>}
          {isError && <p className="mt-4 text-text-muted">Couldn&apos;t load usage right now.</p>}

          {quota && (
            <div className="mt-4 space-y-6 rounded-lg border border-border p-6">
              <Meter label="Questions asked" used={quota.questions.used} limit={quota.questions.limit} />
              <Meter label="Files uploaded"  used={quota.files.used}     limit={quota.files.limit} />
              <Meter label="Storage used"    used={quota.bytesStored.used} limit={quota.bytesStored.limit} format={fileSize} />
              <Meter label="Tokens consumed" used={quota.tokens.used}    limit={quota.tokens.limit} />
            </div>
          )}
        </section>

        {/* ── session ─────────────────────────────────────── */}
        <section className="mt-12">
          <h2 className="text-sm uppercase tracking-widest text-text-faint">Session</h2>
          <div className="mt-4 flex items-center justify-between rounded-lg border border-border p-6">
            <div>
              <p className="text-text">Sign out of TAssist</p>
              <p className="mt-1 text-sm text-text-muted">Ends this session on this device.</p>
            </div>
            <Button variant="danger" onClick={handleSignOut}>
              <LogOut size={16} strokeWidth={1.75} /> Sign out
            </Button>
          </div>
        </section>
      </main>
    </AppLayout>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between px-6 py-4">
      <dt className="text-text-muted">{label}</dt>
      <dd className="text-text">{value}</dd>
    </div>
  )
}

function Meter({
  label, used, limit, format = (n: number) => n.toLocaleString(),
}: {
  label: string; used: number; limit: number; format?: (n: number) => string
}) {
  // limit <= 0 means "no limit configured" — show usage without a bar.
  const capped = limit > 0
  const pct = capped ? Math.min(100, Math.round((used / limit) * 100)) : 0
  const near = pct >= 80

  return (
    <div>
      <div className="flex items-baseline justify-between text-sm">
        <span className="text-text">{label}</span>
        <span className={near ? 'text-accent-amber' : 'text-text-muted'}>
          {format(used)}{capped && <span className="text-text-faint"> / {format(limit)}</span>}
        </span>
      </div>
      {capped && (
        <div className="mt-2 h-2 overflow-hidden rounded-round bg-bg-sunken">
          <div
            className={near ? 'h-full bg-accent-amber' : 'h-full bg-primary'}
            style={{ width: `${pct}%` }}
            role="progressbar"
            aria-valuenow={pct}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={label}
          />
        </div>
      )}
    </div>
  )
}
