import type { SourceItem } from '@/lib/sse/types'

export function SourcesStrip({ sources, onOpen }: { sources: SourceItem[]; onOpen?: (num: number) => void }) {
  if (sources.length === 0) return null
  return (
    <div className="mb-2 flex flex-wrap gap-1.5">
      {sources.map((s) => (
        <button
          key={s.num}
          onClick={() => onOpen?.(s.num)}
          className="inline-flex items-center gap-1 rounded-round border border-border bg-bg-elev px-2 py-0.5 text-xs text-text-muted hover:border-primary hover:text-primary"
          title={s.snippet}
        >
          <span className="font-medium text-primary">S{s.num}</span> {s.label}
        </button>
      ))}
    </div>
  )
}
