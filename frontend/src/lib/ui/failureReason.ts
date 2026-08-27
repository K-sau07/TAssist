// Maps raw backend/provider failure strings to short, human messages for the UI.
// The backend currently stores the raw provider response (e.g. a Voyage 429 JSON blob);
// until that's cleaned up server-side (Phase 2), we translate known cases here and fall
// back to a calm generic message so users never see raw JSON.

export function friendlyFailureReason(raw: string | null | undefined): string {
  const fallback = 'We couldn’t process this file. Please try again in a moment.'
  if (!raw) return fallback
  const s = raw.toLowerCase()

  // Embedding/provider rate limit (Voyage 429).
  if (s.includes('429') || s.includes('too many requests') || s.includes('rate limit')) {
    return 'The document service is busy right now (rate limit). Try again in a minute, or upload a smaller file.'
  }
  // Payment/quota wording that can accompany a 429.
  if (s.includes('payment method') || s.includes('rate limits of')) {
    return 'This file is too large for the current processing limit. Try a smaller file, or try again shortly.'
  }
  // File too large / size.
  if (s.includes('too large') || s.includes('413') || s.includes('payload too large') || s.includes('max') && s.includes('size')) {
    return 'This file is too large to process. Please upload a smaller document.'
  }
  // Unsupported / parse failures.
  if (s.includes('unsupported') || s.includes('parse') || s.includes('extract') || s.includes('corrupt')) {
    return 'We couldn’t read this file. Make sure it’s a valid, text-based PDF and try again.'
  }
  // Timeouts / transient network.
  if (s.includes('timeout') || s.includes('timed out') || s.includes('unavailable') || s.includes('503')) {
    return 'Processing timed out. Please try again in a moment.'
  }
  return fallback
}
