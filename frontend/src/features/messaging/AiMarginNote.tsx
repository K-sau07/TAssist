import { useState } from 'react'
import { Sparkles, Trash2, BookOpen } from 'lucide-react'
import type { MessageView, CitationView } from '@/lib/api/messaging'
import type { SourceItem } from '@/lib/sse/types'
import { SnippetDrawer } from '@/features/chat/components/SnippetDrawer'
import { Markdown } from '@/design/components/Markdown'
import { dedupeCitations } from '@/lib/ui/citations'
import { isGroundedAi } from './logic'

/**
 * The AI turn as a "margin note" / study annotation — the glow-up signature
 * (05_GLOWUP_DESIGN_BIBLE §B3, §D5; AI-citation canon §A2).
 * Always the calm indigo study-card. When the answer is grounded, it shows a
 * "grounded in N sources" eyebrow + numbered footnote chips → source passage.
 * When it isn't (casual/conversational), it stays a clean AI reply — no sources
 * advertised, no alarm. Sources appear only when they add value.
 */
export function AiMarginNote({
  msg, canDelete, onDelete,
}: { msg: MessageView; canDelete: boolean; onDelete: () => void }) {
  const [source, setSource] = useState<SourceItem | null>(null)
  const grounded = isGroundedAi(msg)
  // Collapse citations from the same file to one entry (retrieval Phase B finding).
  const sources = dedupeCitations(msg.citations)

  const open = (c: CitationView, i: number) =>
    setSource({ num: i + 1, fileId: c.fileId, label: c.displayLabel, snippet: c.snippet ?? '', similarity: null })

  const time = new Date(msg.createdAt).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })

  return (
    <div className="group relative my-2 animate-msg-in">
      <div className="rounded-lg border-l-[3px] border-l-[var(--primary-rule)] bg-primary-wash px-4 py-3 shadow-1">
        {/* eyebrow */}
        <div className="mb-1.5 flex items-center gap-2">
          <span className="inline-flex items-center gap-1 text-2xs font-semibold uppercase tracking-wider text-primary">
            <Sparkles size={12} />
            {grounded
              ? `AI · grounded in ${sources.length} source${sources.length > 1 ? 's' : ''}`
              : 'AI'}
          </span>
          <span className="text-2xs text-text-faint">{time}</span>
        </div>

        {/* answer — typeset like a passage, markdown-rendered */}
        <Markdown>{msg.content}</Markdown>

        {/* sources footer — only when grounded */}
        {grounded && (
          <div className="mt-3 border-t border-border/60 pt-2">
            <p className="mb-1.5 flex items-center gap-1 text-2xs uppercase tracking-wider text-text-faint">
              <BookOpen size={11} /> Sources
            </p>
            <div className="flex flex-wrap gap-1.5">
              {sources.map((c, i) => (
                <button key={c.chunkId + i} onClick={() => open(c, i)}
                  title={c.snippet ? c.snippet.slice(0, 200) : c.displayLabel}
                  className="group/cit inline-flex items-center gap-1.5 rounded-md border border-border bg-bg-elev px-2 py-1 text-xs text-text transition-colors hover:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus">
                  <sup className="font-mono text-[10px] font-semibold text-primary">{i + 1}</sup>
                  <span className="max-w-[220px] truncate">{c.displayLabel}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* hover delete */}
      {canDelete && (
        <div className="absolute right-2 top-0 -translate-y-1/2 opacity-0 transition-opacity group-hover:opacity-100">
          <button onClick={onDelete} title="Delete message"
            className="grid h-7 w-7 place-items-center rounded-md border border-border bg-bg-elev text-text-faint shadow-1 hover:text-danger focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus">
            <Trash2 size={13} />
          </button>
        </div>
      )}

      <SnippetDrawer source={source} onClose={() => setSource(null)} />
    </div>
  )
}
