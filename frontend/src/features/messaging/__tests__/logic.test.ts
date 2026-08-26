import { describe, it, expect } from 'vitest'
import {
  composerSuggestions, buildToken, triggersAi, mergeMessage, applyDeleted, unreadFor,
} from '../logic'
import type { MessageView } from '@/lib/api/messaging'

const msg = (over: Partial<MessageView> = {}): MessageView => ({
  id: 'm1', senderKind: 'HUMAN', sender: { userId: 'u1', displayName: 'Alice' },
  content: 'hi', citations: [], createdAt: '2026-01-01T00:00:00Z', deleted: false, ...over,
})

describe('buildToken', () => {
  it('quotes names with spaces', () => {
    expect(buildToken('Lecture 3')).toBe('@"Lecture 3"')
  })
  it('leaves single tokens unquoted', () => {
    expect(buildToken('slides')).toBe('@slides')
  })
})

describe('triggersAi', () => {
  it('detects standalone @ai and @assist (case-insensitive)', () => {
    expect(triggersAi('hey @ai what is this')).toBe(true)
    expect(triggersAi('@Assist please help')).toBe(true)
    expect(triggersAi('start @AI')).toBe(true)
  })
  it('ignores emails and words containing ai', () => {
    expect(triggersAi('email bob@aixyz.com')).toBe(false)
    expect(triggersAi('going to the @airport')).toBe(false)
    expect(triggersAi('plain message')).toBe(false)
  })
})

describe('composerSuggestions', () => {
  const people = [{ displayName: 'Alice' }, { displayName: 'Bob Stone' }]
  const files = [{ originalFilename: 'Lecture 3.pdf' }, { originalFilename: 'syllabus' }]

  it('offers @ai and @assist first when query matches', () => {
    const s = composerSuggestions('a', people, files)
    expect(s[0].insert).toBe('@ai')
    expect(s[1].insert).toBe('@assist')
  })
  it('suggests participants with quoted tokens for spaced names', () => {
    const s = composerSuggestions('bob', people, files)
    expect(s.find((x) => x.kind === 'participant')?.insert).toBe('@"Bob Stone"')
  })
  it('suggests channel files, quoting spaced labels', () => {
    const s = composerSuggestions('lecture', people, files)
    expect(s.find((x) => x.kind === 'file')?.insert).toBe('@"Lecture 3.pdf"')
  })
  it('caps at 8 suggestions', () => {
    const many = Array.from({ length: 20 }, (_, i) => ({ displayName: `aaa${i}` }))
    expect(composerSuggestions('a', many, []).length).toBeLessThanOrEqual(8)
  })
})

describe('mergeMessage', () => {
  it('appends a new message and keeps oldest-first order', () => {
    const a = msg({ id: 'a', createdAt: '2026-01-01T00:00:00Z' })
    const b = msg({ id: 'b', createdAt: '2026-01-01T00:01:00Z' })
    const out = mergeMessage([b], a)
    expect(out.map((m) => m.id)).toEqual(['a', 'b'])
  })
  it('de-duplicates by id (updates in place)', () => {
    const a = msg({ id: 'a', content: 'old' })
    const out = mergeMessage([a], msg({ id: 'a', content: 'new' }))
    expect(out).toHaveLength(1)
    expect(out[0].content).toBe('new')
  })
})

describe('applyDeleted', () => {
  it('tombstones the matching message', () => {
    const out = applyDeleted([msg({ id: 'a' })], 'a')
    expect(out[0].deleted).toBe(true)
    expect(out[0].content).toBeNull()
    expect(out[0].citations).toEqual([])
  })
})

describe('unreadFor', () => {
  const list = [
    msg({ id: '1', sender: { userId: 'other', displayName: 'X' }, createdAt: '2026-01-01T00:00:00Z' }),
    msg({ id: '2', sender: { userId: 'other', displayName: 'X' }, createdAt: '2026-01-01T00:05:00Z' }),
    msg({ id: '3', sender: { userId: 'me', displayName: 'Me' }, createdAt: '2026-01-01T00:06:00Z' }),
  ]
  it('counts others’ messages after last-read, excluding mine', () => {
    expect(unreadFor(list, 'me', '2026-01-01T00:00:00Z')).toBe(1) // only msg 2
  })
  it('counts all others when never read', () => {
    expect(unreadFor(list, 'me', null)).toBe(2)
  })
  it('ignores deleted messages', () => {
    const withDel = [...list, msg({ id: '4', sender: { userId: 'other', displayName: 'X' }, createdAt: '2026-01-01T01:00:00Z', deleted: true })]
    expect(unreadFor(withDel, 'me', '2026-01-01T00:00:00Z')).toBe(1)
  })
})

import { groupsWith, dayDividerLabel, startsNewDay } from '../logic'

describe('groupsWith', () => {
  const base = (over: Partial<MessageView>): MessageView => msg(over)
  it('groups same human sender within 5 min', () => {
    const a = base({ id: 'a', sender: { userId: 'u1', displayName: 'A' }, createdAt: '2026-01-01T00:00:00Z' })
    const b = base({ id: 'b', sender: { userId: 'u1', displayName: 'A' }, createdAt: '2026-01-01T00:03:00Z' })
    expect(groupsWith(a, b)).toBe(true)
  })
  it('does not group across the time window', () => {
    const a = base({ id: 'a', sender: { userId: 'u1', displayName: 'A' }, createdAt: '2026-01-01T00:00:00Z' })
    const b = base({ id: 'b', sender: { userId: 'u1', displayName: 'A' }, createdAt: '2026-01-01T00:10:00Z' })
    expect(groupsWith(a, b)).toBe(false)
  })
  it('does not group different senders', () => {
    const a = base({ id: 'a', sender: { userId: 'u1', displayName: 'A' } })
    const b = base({ id: 'b', sender: { userId: 'u2', displayName: 'B' } })
    expect(groupsWith(a, b)).toBe(false)
  })
  it('never groups AI turns', () => {
    const a = base({ id: 'a', senderKind: 'AI', sender: null })
    const b = base({ id: 'b', senderKind: 'AI', sender: null })
    expect(groupsWith(a, b)).toBe(false)
  })
  it('no previous → not grouped', () => {
    expect(groupsWith(undefined, base({ id: 'a' }))).toBe(false)
  })
  it('deleted messages do not group', () => {
    const a = base({ id: 'a', sender: { userId: 'u1', displayName: 'A' }, deleted: true })
    const b = base({ id: 'b', sender: { userId: 'u1', displayName: 'A' } })
    expect(groupsWith(a, b)).toBe(false)
  })
})

describe('dayDividerLabel', () => {
  const now = new Date('2026-03-05T12:00:00Z')
  it('Today / Yesterday', () => {
    expect(dayDividerLabel('2026-03-05T09:00:00Z', now)).toBe('Today')
    expect(dayDividerLabel('2026-03-04T09:00:00Z', now)).toBe('Yesterday')
  })
  it('same-year date without year', () => {
    expect(dayDividerLabel('2026-03-01T09:00:00Z', now)).toMatch(/Mar 1/)
  })
  it('prior-year date includes year', () => {
    expect(dayDividerLabel('2025-12-25T09:00:00Z', now)).toMatch(/2025/)
  })
})

describe('startsNewDay', () => {
  it('true when no previous', () => {
    expect(startsNewDay(undefined, msg({ id: 'a' }))).toBe(true)
  })
  it('true across a day boundary, false within a day', () => {
    // Local-time (no Z) so the day boundary is unambiguous regardless of runner TZ.
    const d1 = msg({ id: 'a', createdAt: '2026-03-04T23:00:00' })
    const d2 = msg({ id: 'b', createdAt: '2026-03-05T01:00:00' })
    const d3 = msg({ id: 'c', createdAt: '2026-03-05T02:00:00' })
    expect(startsNewDay(d1, d2)).toBe(true)
    expect(startsNewDay(d2, d3)).toBe(false)
  })
})
