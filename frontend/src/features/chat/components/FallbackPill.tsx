import { Sparkles } from 'lucide-react'
export function FallbackPill() {
  return (
    <div className="mb-2 inline-flex items-center gap-1.5 rounded-round bg-accent-amber/20 px-2.5 py-1 text-xs text-text">
      <Sparkles size={13} strokeWidth={1.75} /> Not from your documents — general AI
    </div>
  )
}
