import { motion, useReducedMotion } from 'framer-motion'

/**
 * Signature hero animation (§15.6): chubby file-characters floating on a warm shadow,
 * with speech bubbles drifting upward. Custom SVG + Framer Motion (no external Lottie asset),
 * so it respects prefers-reduced-motion — reduced amplitude, no infinite loop.
 */
export function HeroAnimation() {
  const reduce = useReducedMotion()
  const float = (delay: number, amp = 10) =>
    reduce ? {} : {
      animate: { y: [0, -amp, 0] },
      transition: { duration: 4, repeat: Infinity, ease: 'easeInOut', delay },
    }

  return (
    <svg viewBox="0 0 420 380" className="h-full w-full" role="img" aria-label="Illustration of documents answering questions">
      {/* soft ground shadow */}
      <ellipse cx="210" cy="340" rx="120" ry="16" fill="#1E1B2E" opacity="0.06" />

      {/* back file */}
      <motion.g {...float(0.4, 8)}>
        <FileChar x={70} y={150} fill="#A8E6C6" tilt={-8} />
      </motion.g>
      {/* front-left file */}
      <motion.g {...float(0, 12)}>
        <FileChar x={150} y={175} fill="#FFB199" tilt={4} face />
      </motion.g>
      {/* right file */}
      <motion.g {...float(0.8, 10)}>
        <FileChar x={250} y={155} fill="#FFFFFF" tilt={7} />
      </motion.g>

      {/* speech bubbles drifting up */}
      {!reduce && [0, 1, 2].map((i) => (
        <motion.g key={i}
          initial={{ opacity: 0, y: 0 }}
          animate={{ opacity: [0, 1, 0], y: [-0, -70] }}
          transition={{ duration: 3.5, repeat: Infinity, delay: i * 1.2, ease: 'easeOut' }}
        >
          <circle cx={200 + i * 34} cy={140 - i * 8} r={12 + i * 2} fill="#3E2A93" opacity="0.9" />
          <circle cx={200 + i * 34} cy={140 - i * 8} r={3} fill="#FDFAF4" />
          <circle cx={208 + i * 34} cy={140 - i * 8} r={3} fill="#FDFAF4" />
        </motion.g>
      ))}
    </svg>
  )
}

function FileChar({ x, y, fill, tilt, face }: { x: number; y: number; fill: string; tilt: number; face?: boolean }) {
  return (
    <g transform={`translate(${x} ${y}) rotate(${tilt})`}>
      <rect width="90" height="112" rx="16" fill={fill} stroke="#1E1B2E" strokeOpacity="0.08" />
      <rect x="16" y="24" width="58" height="7" rx="3.5" fill="#1E1B2E" opacity="0.12" />
      <rect x="16" y="40" width="44" height="7" rx="3.5" fill="#1E1B2E" opacity="0.12" />
      <rect x="16" y="56" width="52" height="7" rx="3.5" fill="#1E1B2E" opacity="0.12" />
      {face && (
        <g transform="translate(0 6)">
          <circle cx="34" cy="78" r="4" fill="#1E1B2E" />
          <circle cx="56" cy="78" r="4" fill="#1E1B2E" />
          <path d="M34 90 Q45 98 56 90" stroke="#1E1B2E" strokeWidth="3" fill="none" strokeLinecap="round" />
        </g>
      )}
    </g>
  )
}
