// Auth API (spec §12.1). Matches backend AuthResponse { user, token, expiresAt }.
import { apiFetch } from './client'
import type { CurrentUser } from '@/lib/auth/store'

export interface AuthResponse {
  user: CurrentUser
  token: string
  expiresAt: string
}

export function signup(email: string, displayName: string, password: string) {
  return apiFetch<AuthResponse>('/auth/signup', {
    method: 'POST',
    auth: false,
    body: { email, displayName, password },
  })
}

export function login(email: string, password: string) {
  return apiFetch<AuthResponse>('/auth/login', {
    method: 'POST',
    auth: false,
    body: { email, password },
  })
}

export function logout() {
  return apiFetch<void>('/auth/logout', { method: 'POST' })
}

/** Current user (spec §12.1 GET /api/me). Used by OAuth-complete to hydrate the store. */
export function me() {
  return apiFetch<CurrentUser>('/me')
}

/** Google OAuth: navigating here hits the backend, which 302s to Google consent,
 *  then back to /auth/complete?token=<JWT> (spec §12.1 / §14.4). */
export const googleAuthorizeUrl = '/api/auth/google/authorize' 
