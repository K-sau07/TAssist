import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthCard, FieldError } from './AuthCard'
import { Input } from '@/design/components/Input'
import { Button } from '@/design/components/Button'
import { login } from '@/lib/api/auth'
import { ApiError } from '@/lib/api/client'
import { useAuthStore } from '@/lib/auth/store'

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})
type FormValues = z.infer<typeof schema>

export default function LoginPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const setSession = useAuthStore((s) => s.setSession)
  const oauthFailed = params.get('error') === 'oauth_failed'
  const [submitError, setSubmitError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } =
    useForm<FormValues>({ resolver: zodResolver(schema) })

  async function onSubmit(values: FormValues) {
    setSubmitError(null)
    try {
      const res = await login(values.email, values.password)
      setSession(res.token, res.user)
      navigate(params.get('next') || '/app', { replace: true })
    } catch (e) {
      setSubmitError(e instanceof ApiError && e.status === 401
        ? 'Incorrect email or password.'
        : e instanceof ApiError ? e.message : 'Something went wrong. Try again.')
    }
  }

  return (
    <AuthCard
      title="Welcome back"
      footer={<>New here? <Link to="/signup" className="text-primary hover:underline">Create an account</Link></>}
    >
      {oauthFailed && (
        <p className="mb-4 rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
          Google sign-in didn't complete. Try again or use email.
        </p>
      )}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div>
          <Input type="email" placeholder="Email" autoComplete="email" {...register('email')} />
          <FieldError msg={errors.email?.message} />
        </div>
        <div>
          <Input type="password" placeholder="Password" autoComplete="current-password" {...register('password')} />
          <FieldError msg={errors.password?.message} />
        </div>
        {submitError && <p className="text-sm text-danger">{submitError}</p>}
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? 'Logging in…' : 'Log in'}
        </Button>
      </form>
    </AuthCard>
  )
}
