// Global auth store (spec §13.3): the ONLY global Zustand store. token + currentUser.
import { create } from 'zustand'

export interface CurrentUser {
  id: string
  email: string
  displayName: string
}

interface AuthState {
  token: string | null
  user: CurrentUser | null
  setSession: (token: string, user: CurrentUser) => void
  setToken: (token: string) => void
  clear: () => void
  isAuthenticated: () => boolean
}

const TOKEN_KEY = 'tassist.token'
const USER_KEY = 'tassist.user'

function loadUser(): CurrentUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as CurrentUser) : null
  } catch { return null }
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: localStorage.getItem(TOKEN_KEY),
  user: loadUser(),
  setSession: (token, user) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    set({ token, user })
  },
  setToken: (token) => {
    localStorage.setItem(TOKEN_KEY, token)
    set({ token })
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    set({ token: null, user: null })
  },
  isAuthenticated: () => Boolean(get().token),
}))
