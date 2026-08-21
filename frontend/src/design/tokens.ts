// Motion + scale tokens for JS/Framer Motion use. Mirror of theme.css (spec §15.4–15.5).
export const spring = {
  soft:   { type: 'spring', stiffness: 260, damping: 24, mass: 0.8 } as const,
  snappy: { type: 'spring', stiffness: 420, damping: 32 } as const,
  lazy:   { type: 'spring', stiffness: 140, damping: 22 } as const,
}

export const radius = { sm: 6, md: 10, lg: 16, xl: 24, round: 999 } as const

// Spacing scale (px) — never use arbitrary values (spec §15.4).
export const space = [4, 8, 12, 16, 24, 32, 48, 64, 96] as const

export const pageTransition = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -8 },
  transition: { duration: 0.22 },
}
