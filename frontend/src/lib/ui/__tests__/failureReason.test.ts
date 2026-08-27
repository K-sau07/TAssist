import { describe, it, expect } from 'vitest'
import { friendlyFailureReason } from '../failureReason'

describe('friendlyFailureReason', () => {
  it('maps a Voyage 429 blob to a rate-limit message', () => {
    const raw = '429 Too Many Requests: {"detail":"You have not yet added your payment method ... reduced rate limits of 3 RPM and 10K TPM ..."}'
    expect(friendlyFailureReason(raw)).toMatch(/rate limit|smaller file/i)
    expect(friendlyFailureReason(raw)).not.toMatch(/\{|detail|RPM/)
  })
  it('maps too-large errors', () => {
    expect(friendlyFailureReason('413 Payload Too Large')).toMatch(/too large/i)
  })
  it('maps parse/extract failures', () => {
    expect(friendlyFailureReason('Failed to extract text: corrupt PDF')).toMatch(/couldn’t read|valid/i)
  })
  it('maps timeouts', () => {
    expect(friendlyFailureReason('Upstream 503 service unavailable')).toMatch(/try again|timed out/i)
  })
  it('falls back for null / unknown', () => {
    expect(friendlyFailureReason(null)).toMatch(/couldn’t process/i)
    expect(friendlyFailureReason('some weird thing')).toMatch(/couldn’t process/i)
  })
  it('never leaks raw JSON braces', () => {
    expect(friendlyFailureReason('{"detail":"x"}')).not.toContain('{')
  })
})
