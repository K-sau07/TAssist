import { X } from 'lucide-react'
import type { SourceItem } from '@/lib/sse/types'

export function SnippetDrawer({ source, onClose }: { source: SourceItem | null; onClose: () => void }) {
  if (!source) return null
  return (
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div className="fixed inset-0 z-40 flex justify-end bg-black/20"
         role="dialog" aria-modal="true" aria-label={`Source S${source.num}`}
         onClick={onClose} onKeyDown={(e) => { if (e.key === 'Escape') onClose() }} tabIndex={-1}>
      {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions */}
      <aside className="h-full w-full max-w-md overflow-y-auto border-l border-border bg-bg-elev p-6 shadow-2"
             role="document" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-wider text-text-faint">Source S{source.num}</p>
            <h3 className="mt-1 text-lg">{source.label}</h3>
          </div>
          <button onClick={onClose} className="text-text-faint hover:text-text"><X size={18} /></button>
        </div>
        {source.similarity != null && (
          <p className="mb-3 text-xs text-text-faint">Relevance {(source.similarity * 100).toFixed(0)}%</p>
        )}
        <p className="whitespace-pre-wrap rounded-md bg-bg-sunken p-4 text-sm leading-relaxed text-text">
          {source.snippet}
        </p>
      </aside>
    </div>
  )
}
