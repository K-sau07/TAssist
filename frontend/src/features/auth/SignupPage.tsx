import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthCard, FieldError } from './AuthCard'
import { Input } from '@/design/components/Input'
import { Button } from '@/design/components/Button'
import { signup } from '@/lib/api/auth'
import { ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/lib/auth/store'

const schema = z.object({
  displayName: z.string().min(1, 'Display name is required').max(80),
  email: z.string().email('Enter a valid email'),
  password: z.string().min(8, 'At least 8 characters'),
})
type FormValues = z.infer<typeof schema>

export default function SignupPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const setSession = useAuthStore((s) => s.setSession)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } =
    useForm<FormValues>({ resolver: zodResolver(schema) })

  async function onSubmit(values: FormValues) {
    setSubmitError(null)
    try {
      const res = await signup(values.email, values.displayName, values.password)
      setSession(res.token, res.user)
      navigate(params.get('next') || '/app', { replace: true })
    } catch (e) {
      setSubmitError(e instanceof ApiError ? e.message : 'Something went wrong. Try again.')
    }
  }

  return (
    <AuthCard
      title="Create your account"
      subtitle="Upload once. Ask anything. Grounded in your own documents."
      footer={<>Already have an account? <Link to="/login" className="text-primary hover:underline">Log in</Link></>}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div>
          <Input placeholder="Display name" autoComplete="name" {...register('displayName')} />
          <FieldError msg={errors.displayName?.message} />
        </div>
        <div>
          <Input type="email" placeholder="Email" autoComplete="email" {...register('email')} />
          <FieldError msg={errors.email?.message} />
        </div>
        <div>
          <Input type="password" placeholder="Password" autoComplete="new-password" {...register('password')} />
          <FieldError msg={errors.password?.message} />
        </div>
        {submitError && <p className="text-sm text-danger">{submitError}</p>}
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </Button>
      </form>
    </AuthCard>
  )
}
