import { AppLayout } from './shell/AppLayout'
import { FileGrid } from '@/features/files/FileGrid'
import { UploadZone } from '@/features/files/UploadZone'
import { useFilesQuery } from '@/lib/hooks/useFiles'
import { useAuthStore } from '@/lib/auth/store'

function greeting() {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening'
}

export default function Dashboard() {
  const user = useAuthStore((s) => s.user)
  const { data: files = [], isLoading } = useFilesQuery()

  return (
    <AppLayout>
      <main className="mx-auto max-w-6xl px-8 py-8">
        <header className="mb-8">
          <h1 className="text-3xl">{greeting()}{user ? `, ${user.displayName}` : ''}</h1>
          <p className="mt-1 text-text-muted">Your files. One brain. Zero uploads to Claude.</p>
        </header>

        <section>
          <h2 className="mb-4 text-xl">Files</h2>
          {isLoading
            ? <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {Array.from({ length: 4 }).map((_, i) => (
                  <div key={i} className="h-32 animate-pulse rounded-lg border border-border bg-bg-sunken" />
                ))}
              </div>
            : <FileGrid files={files} />}
        </section>
      </main>
      <UploadZone />
    </AppLayout>
  )
}
