import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AppLayout } from '@/features/dashboard/shell/AppLayout'
import { Input } from '@/design/components/Input'
import { Button } from '@/design/components/Button'
import { createChannel, searchChannels, type Visibility } from '@/lib/api/channels'
import { ApiError } from '@/lib/api/client'

const schema = z.object({
  displayName: z.string().min(1, 'Display name is required').max(80),
  username: z.string().min(3, 'At least 3 characters').regex(/^[a-z0-9-]+$/, 'Lowercase letters, numbers, hyphens only'),
  description: z.string().max(500).optional(),
  expectationSummary: z.string().max(500).optional(),
  visibility: z.enum(['PUBLIC', 'UNLISTED', 'PRIVATE']),
  requireMessageOnReRequest: z.boolean().optional(),
})
type FormValues = z.infer<typeof schema>

export default function CreateChannelPage() {
  const navigate = useNavigate()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [nameStatus, setNameStatus] = useState<'idle' | 'checking' | 'taken' | 'free'>('idle')
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } =
    useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { visibility: 'PUBLIC' } })

  async function checkUsername() {
    const u = watch('username')?.trim().toLowerCase()
    if (!u || u.length < 3) { setNameStatus('idle'); return }
    setNameStatus('checking')
    try {
      const matches = await searchChannels(u)
      setNameStatus(matches.some((m) => m.username === u) ? 'taken' : 'free')
    } catch { setNameStatus('idle') }
  }

  async function onSubmit(values: FormValues) {
    setSubmitError(null)
    try {
      const c = await createChannel({
        username: values.username.toLowerCase(),
        displayName: values.displayName,
        description: values.description ?? '',
        expectationSummary: values.expectationSummary ?? '',
        visibility: values.visibility as Visibility,
        requireMessageOnReRequest: values.requireMessageOnReRequest ?? false,
      })
      navigate(`/app/channels/${c.id}/manage`)
    } catch (e) {
      setSubmitError(e instanceof ApiError ? e.message : 'Could not create channel.')
    }
  }

  return (
    <AppLayout>
      <main className="mx-auto max-w-xl px-8 py-10">
        <h1 className="text-3xl">Create channel</h1>
        <p className="mt-1 text-text-muted">A public Q&A surface over a subset of your files.</p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5" noValidate>
          <div>
            <span className="mb-1 block text-sm font-medium">Display name</span>
            <Input placeholder="CS101 Help Desk" {...register('displayName')} />
            {errors.displayName && <p className="mt-1 text-xs text-danger">{errors.displayName.message}</p>}
          </div>
          <div>
            <span className="mb-1 block text-sm font-medium">Username</span>
            <div className="flex items-center gap-2">
              <span className="text-text-faint">@</span>
              <Input placeholder="cs101" {...register('username')} onBlur={checkUsername} />
            </div>
            {errors.username && <p className="mt-1 text-xs text-danger">{errors.username.message}</p>}
            {nameStatus === 'checking' && <p className="mt-1 text-xs text-text-faint">Checking availability…</p>}
            {nameStatus === 'taken' && <p className="mt-1 text-xs text-danger">That username is taken.</p>}
            {nameStatus === 'free' && <p className="mt-1 text-xs text-success">Available!</p>}
          </div>
          <div>
            <span className="mb-1 block text-sm font-medium">Description</span>
            <textarea rows={2} className="w-full rounded-md border border-border bg-bg-elev px-3 py-2 text-md outline-none focus:border-primary"
              placeholder="What this channel is about" {...register('description')} />
          </div>
          <div>
            <span className="mb-1 block text-sm font-medium">What to expect</span>
            <textarea rows={2} className="w-full rounded-md border border-border bg-bg-elev px-3 py-2 text-md outline-none focus:border-primary"
              placeholder="Set expectations for people asking questions" {...register('expectationSummary')} />
          </div>
          <div>
            <span className="mb-1 block text-sm font-medium">Visibility</span>
            <select className="w-full rounded-md border border-border bg-bg-elev px-3 py-2 text-md outline-none focus:border-primary" {...register('visibility')}>
              <option value="PUBLIC">Public — listed in Discover</option>
              <option value="UNLISTED">Unlisted — link only</option>
              <option value="PRIVATE">Private — invite only</option>
            </select>
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...register('requireMessageOnReRequest')} />
            Require a message when someone re-requests access
          </label>

          {submitError && <p className="text-sm text-danger">{submitError}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={() => navigate('/app/channels')}>Cancel</Button>
            <Button type="submit" disabled={isSubmitting || nameStatus === 'taken'}>
              {isSubmitting ? 'Creating…' : 'Create channel'}
            </Button>
          </div>
        </form>
      </main>
    </AppLayout>
  )
}
