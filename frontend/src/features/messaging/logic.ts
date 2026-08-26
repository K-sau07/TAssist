// Pure logic for messaging UI — kept framework-free so it's unit-testable (the
// "both sides tested" standard). Composer suggestions + SSE message reconciliation.
import type { MessageView } from '@/lib/api/messaging'

export interface Suggestion {
  kind: 'ai' | 'participant' | 'file'
  label: string // shown in the dropdown
  insert: string // the @token to insert (no trailing space)
}

/**
 * Build composer suggestions for an active @token: @ai/@assist first, then
 * participants, then channel files — filtered by the typed query.
 */
export function composerSuggestions(
  query: string,
  participants: Array<{ displayName: string }>,
  files: Array<{ originalFilename: string }>,
): Suggestion[] {
  const q = query.toLowerCase()
  const out: Suggestion[] = []

  for (const s of ['ai', 'assist'] as const) {
    if (s.startsWith(q)) {
      out.push({ kind: 'ai', label: `@${s} — grounded answer from channel docs`, insert: `@${s}` })
    }
  }
  for (const p of participants) {
    if (p.displayName.toLowerCase().includes(q)) {
      out.push({ kind: 'participant', label: p.displayName, insert: buildToken(p.displayName) })
    }
  }
  for (const f of files) {
    if (f.originalFilename.toLowerCase().includes(q)) {
      out.push({ kind: 'file', label: f.originalFilename, insert: buildToken(f.originalFilename) })
    }
  }
  return out.slice(0, 8)
}

/** Quote a token if it contains whitespace, matching the backend mention parser. */
export function buildToken(name: string): string {
  return /\s/.test(name) ? `@"${name}"` : `@${name}`
}

/** True if the text will trigger a grounded AI turn (@ai/@assist standalone). Mirrors backend regex. */
export function triggersAi(text: string): boolean {
  return /(?:^|[^\w@])@(ai|assist)\b/i.test(text)
}

/**
 * Merge an incoming message into a list, keeping oldest-first order and avoiding
 * duplicates (our own POST-returned message may already be present).
 */
export function mergeMessage(list: MessageView[], incoming: MessageView): MessageView[] {
  if (list.some((m) => m.id === incoming.id)) {
    return list.map((m) => (m.id === incoming.id ? incoming : m))
  }
  const next = [...list, incoming]
  next.sort((a, b) => a.createdAt.localeCompare(b.createdAt))
  return next
}

/** Apply a soft-delete event: turn the matching message into a tombstone. */
export function applyDeleted(list: MessageView[], messageId: string): MessageView[] {
  return list.map((m) =>
    m.id === messageId ? { ...m, deleted: true, content: null, citations: [] } : m,
  )
}

/** Count messages after last-read that aren't mine and aren't deleted. */
export function unreadFor(list: MessageView[], myUserId: string, lastReadIso: string | null): number {
  const since = lastReadIso ? new Date(lastReadIso).getTime() : 0
  return list.filter(
    (m) => !m.deleted && m.sender?.userId !== myUserId && new Date(m.createdAt).getTime() > since,
  ).length
}

// ── G-UI1: Slack-style row grouping + day dividers (05_GLOWUP §D5) ──

/**
 * Should `cur` collapse under `prev` (same author, close in time, both human)?
 * Grouped messages hide the repeated avatar/name/timestamp for a tighter thread,
 * exactly like Slack. AI turns never group (each is a standalone "margin note").
 */
export function groupsWith(
  prev: MessageView | undefined,
  cur: MessageView,
  windowMs = 5 * 60 * 1000,
): boolean {
  if (!prev) return false
  if (prev.senderKind !== 'HUMAN' || cur.senderKind !== 'HUMAN') return false
  if (prev.deleted || cur.deleted) return false
  if (prev.sender?.userId == null || prev.sender.userId !== cur.sender?.userId) return false
  const dt = new Date(cur.createdAt).getTime() - new Date(prev.createdAt).getTime()
  return dt >= 0 && dt <= windowMs
}

/**
 * Label for a day-divider between messages: "Today" / "Yesterday" / "Mar 3"
 * (year added only when it differs from now). `now` injectable for tests.
 */
export function dayDividerLabel(iso: string, now: Date = new Date()): string {
  const d = new Date(iso)
  const startOf = (x: Date) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const dayMs = 86_400_000
  const diffDays = Math.round((startOf(now) - startOf(d)) / dayMs)
  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'
  const sameYear = d.getFullYear() === now.getFullYear()
  return d.toLocaleDateString('en-US', sameYear
    ? { month: 'short', day: 'numeric' }
    : { month: 'short', day: 'numeric', year: 'numeric' })
}

/** True if `cur` starts a new calendar day vs `prev` (→ render a day divider before it). */
export function startsNewDay(prev: MessageView | undefined, cur: MessageView): boolean {
  if (!prev) return true
  const a = new Date(prev.createdAt), b = new Date(cur.createdAt)
  return a.getFullYear() !== b.getFullYear() || a.getMonth() !== b.getMonth() || a.getDate() !== b.getDate()
}

// ── G-UI2: AI margin-note grounding state (05_GLOWUP §D5, §A2 missing-source disclosure) ──

/**
 * Is an AI answer grounded (backed by retrieved sources) or a fallback?
 * Our RAG guarantee: a grounded answer always carries citations; a fallback
 * ("not in your documents") carries none. We render grounded = indigo, fallback
 * = honest amber — never styled identically (the anti-hallucination pattern).
 */
export function isGroundedAi(msg: MessageView): boolean {
  return msg.senderKind === 'AI' && msg.citations.length > 0
}
