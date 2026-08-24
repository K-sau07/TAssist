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
