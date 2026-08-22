import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { me } from '@/lib/api/auth'
import { useAuthStore } from '@/lib/auth/store'

export default function OAuthCompletePage() {
  const navigate = useNavigate()
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return // guard StrictMode double-invoke
    ran.current = true

    const params = new URLSearchParams(window.location.search)
    const token = params.get('token')
    if (!token) { navigate('/login?error=oauth_failed', { replace: true }); return }

    const { setToken, setSession, clear } = useAuthStore.getState()
    setToken(token) // so the /me call is authorized
    ;(async () => {
      try {
        const user = await me()
        setSession(token, user)
        window.history.replaceState({}, '', '/auth/complete') // strip token from URL
        navigate('/app', { replace: true })
      } catch {
        clear()
        navigate('/login?error=oauth_failed', { replace: true })
      }
    })()
  }, [navigate])

  return (
    <main className="min-h-screen grid place-items-center bg-bg">
      <div className="flex flex-col items-center gap-4 text-text-muted">
        <div className="h-8 w-8 animate-spin rounded-round border-2 border-border border-t-primary" />
        <p>Signing you in…</p>
      </div>
    </main>
  )
}
