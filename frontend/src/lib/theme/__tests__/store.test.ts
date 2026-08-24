import { describe, it, expect, beforeEach } from 'vitest'
import { useThemeStore } from '../store'

describe('theme store', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    useThemeStore.setState({ theme: 'light' })
  })

  it('setTheme(dark) applies data-theme and persists', () => {
    useThemeStore.getState().setTheme('dark')
    expect(useThemeStore.getState().theme).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(localStorage.getItem('tassist.theme')).toBe('dark')
  })

  it('setTheme(light) removes data-theme attribute', () => {
    useThemeStore.getState().setTheme('dark')
    useThemeStore.getState().setTheme('light')
    expect(document.documentElement.getAttribute('data-theme')).toBeNull()
    expect(localStorage.getItem('tassist.theme')).toBe('light')
  })

  it('toggle flips between light and dark', () => {
    useThemeStore.setState({ theme: 'light' })
    useThemeStore.getState().toggle()
    expect(useThemeStore.getState().theme).toBe('dark')
    useThemeStore.getState().toggle()
    expect(useThemeStore.getState().theme).toBe('light')
  })
})
