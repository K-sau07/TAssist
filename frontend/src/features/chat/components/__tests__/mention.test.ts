import { describe, it, expect } from 'vitest'
import { activeToken, buildMention } from '../useMentionPicker'

describe('buildMention', () => {
  it('wraps names with spaces in quotes', () => {
    expect(buildMention('My Report.pdf')).toBe('@"My Report.pdf"')
  })
  it('leaves single-word names unquoted', () => {
    expect(buildMention('resume.pdf')).toBe('@resume.pdf')
  })
})

describe('activeToken', () => {
  it('detects an @token at the caret', () => {
    const text = 'hello @res'
    const t = activeToken(text, text.length)
    expect(t).toEqual({ query: 'res', start: 6 })
  })
  it('detects an @token at the very start', () => {
    const t = activeToken('@abc', 4)
    expect(t).toEqual({ query: 'abc', start: 0 })
  })
  it('returns null when caret is after whitespace following the token', () => {
    // "@res " then caret at end → whitespace breaks the token
    const text = '@res '
    expect(activeToken(text, text.length)).toBeNull()
  })
  it('returns null for an @ mid-word (e.g. an email)', () => {
    const text = 'mail bob@site'
    // caret right after "bob@site" — the '@' is preceded by 'b', not whitespace
    expect(activeToken(text, text.length)).toBeNull()
  })
  it('returns null when there is no @ before the caret', () => {
    expect(activeToken('just text', 9)).toBeNull()
  })
  it('captures an empty query right after typing @', () => {
    const t = activeToken('hey @', 5)
    expect(t).toEqual({ query: '', start: 4 })
  })
})
