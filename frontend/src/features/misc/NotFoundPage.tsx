import { Link } from 'react-router-dom'
import { BookOpen } from 'lucide-react'
import { Button } from '@/design/components/Button'

/** Warm, on-brand 404 — an invitation back, not a cold error (05_GLOWUP §D6). */
export default function NotFoundPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-bg px-6">
      <div className="max-w-md text-center">
        <div className="mx-auto grid h-14 w-14 place-items-center rounded-lg bg-primary-wash text-primary">
          <BookOpen size={26} strokeWidth={1.6} />
        </div>
        <h1 className="mt-5 font-display text-3xl text-text">This page isn’t in your notes</h1>
        <p className="mt-2 text-text-muted">
          The page you’re looking for doesn’t exist or may have moved. Let’s get you back to your desk.
        </p>
        <Link to="/app" className="mt-6 inline-block">
          <Button>Back to your desk</Button>
        </Link>
      </div>
    </main>
  )
}
