import { initials, avatarColorVar } from '@/lib/ui/avatar'

/**
 * Initials avatar tile (05_GLOWUP §C1, D-05-4). Deterministic tint from the name,
 * so the same person is always the same colour. No uploaded images.
 * AI gets a distinct indigo "spark" tile instead of initials.
 */
export function Avatar({ name, size = 28, ai = false }: { name?: string; size?: number; ai?: boolean }) {
  const px = { width: size, height: size, fontSize: Math.round(size * 0.4) }

  if (ai) {
    return (
      <span
        aria-hidden
        className="grid shrink-0 place-items-center rounded-round font-semibold text-primary-fg"
        style={{ ...px, background: 'var(--tassist-primary)' }}
      >
        ✦
      </span>
    )
  }

  const label = name?.trim() || 'Unknown'
  return (
    <span
      aria-hidden
      className="grid shrink-0 place-items-center rounded-round font-semibold text-white"
      style={{ ...px, background: avatarColorVar(label) }}
      title={label}
    >
      {initials(label)}
    </span>
  )
}
