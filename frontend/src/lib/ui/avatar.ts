// Deterministic avatar helpers (05_GLOWUP_DESIGN_BIBLE §C1, D-05-4).
// No uploaded avatars — initials tiles tinted by a stable hash of the display name,
// so the same person always gets the same colour across the app.

/** Up to two initials from a display name. "Saurabh Kashyap" -> "SK", "alice" -> "A". */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase()
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
}

/** Stable non-negative hash of a string (djb2). Same input -> same output. */
export function hashString(s: string): number {
  let h = 5381
  for (let i = 0; i < s.length; i++) h = ((h << 5) + h + s.charCodeAt(i)) >>> 0
  return h
}

/** 1..6 avatar tint index for a name, mapping to --avatar-{n} tokens. */
export function avatarTint(name: string): number {
  return (hashString(name.trim().toLowerCase()) % 6) + 1
}

/** The CSS var reference for a name's avatar tint, e.g. "var(--avatar-3)". */
export function avatarColorVar(name: string): string {
  return `var(--avatar-${avatarTint(name)})`
}
