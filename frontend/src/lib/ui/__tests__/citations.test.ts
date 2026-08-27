import { describe, it, expect } from 'vitest'
import { dedupeCitations } from '../citations'

const c = (label: string, chunkId: string, snippet: string | null = null) =>
  ({ fileId: 'f-' + label, chunkId, displayLabel: label, snippet })

describe('dedupeCitations', () => {
  it('collapses same-file citations to one entry', () => {
    const out = dedupeCitations([c('Lecture 7', 'a'), c('Lecture 7', 'b'), c('Lecture 7', 'd')])
    expect(out).toHaveLength(1)
    expect(out[0].chunkId).toBe('a') // keeps the first
  })
  it('keeps distinct sources', () => {
    const out = dedupeCitations([c('Lecture 7', 'a'), c('Notes', 'b'), c('Lecture 7', 'd')])
    expect(out.map((x) => x.displayLabel)).toEqual(['Lecture 7', 'Notes'])
  })
  it('preserves first-seen order', () => {
    const out = dedupeCitations([c('B', '1'), c('A', '2'), c('B', '3'), c('C', '4')])
    expect(out.map((x) => x.displayLabel)).toEqual(['B', 'A', 'C'])
  })
  it('handles empty', () => {
    expect(dedupeCitations([])).toEqual([])
  })
})
