import { create } from 'zustand'

export type Theme = 'light' | 'dark'
const KEY = 'tassist.theme'

function initial(): Theme {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved === 'light' || saved === 'dark') return saved
  } catch { /* ignore */ }
  return 'light' // manual toggle only; default light
}

function apply(theme: Theme) {
  const root = document.documentElement
  if (theme === 'dark') root.setAttribute('data-theme', 'dark')
  else root.removeAttribute('data-theme')
}

interface ThemeState {
  theme: Theme
  toggle: () => void
  setTheme: (t: Theme) => void
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: initial(),
  toggle: () => get().setTheme(get().theme === 'dark' ? 'light' : 'dark'),
  setTheme: (t) => {
    try { localStorage.setItem(KEY, t) } catch { /* ignore */ }
    apply(t)
    set({ theme: t })
  },
}))

/** Apply the persisted theme immediately at module load (before React renders) to avoid a flash. */
apply(initial())
