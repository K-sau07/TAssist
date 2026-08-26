import { describe, it, expect } from 'vitest'
import { initials, hashString, avatarTint, avatarColorVar } from '../avatar'

describe('initials', () => {
  it('takes first + last initial for multi-word names', () => {
    expect(initials('Saurabh Kashyap')).toBe('SK')
  })
  it('takes a single initial for one-word names', () => {
    expect(initials('alice')).toBe('A')
  })
  it('uses first and last of 3+ word names', () => {
    expect(initials('Ada B. Lovelace')).toBe('AL')
  })
  it('handles empty / whitespace gracefully', () => {
    expect(initials('')).toBe('?')
    expect(initials('   ')).toBe('?')
  })
})

describe('hashString', () => {
  it('is deterministic', () => {
    expect(hashString('csye7230')).toBe(hashString('csye7230'))
  })
  it('is non-negative', () => {
    expect(hashString('anything at all')).toBeGreaterThanOrEqual(0)
  })
  it('differs for different inputs', () => {
    expect(hashString('alice')).not.toBe(hashString('bob'))
  })
})

describe('avatarTint', () => {
  it('always returns 1..6', () => {
    for (const n of ['a', 'bob', 'Saurabh Kashyap', 'x y z', 'CSYE7230']) {
      const t = avatarTint(n)
      expect(t).toBeGreaterThanOrEqual(1)
      expect(t).toBeLessThanOrEqual(6)
    }
  })
  it('is case/whitespace stable', () => {
    expect(avatarTint('Saurabh Kashyap')).toBe(avatarTint('  saurabh kashyap  '))
  })
})

describe('avatarColorVar', () => {
  it('maps to an --avatar-N css var', () => {
    expect(avatarColorVar('Alice')).toMatch(/^var\(--avatar-[1-6]\)$/)
  })
})
