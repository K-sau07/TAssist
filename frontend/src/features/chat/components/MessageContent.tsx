import { Markdown } from '@/design/components/Markdown'

/**
 * Renders AI answer text as Markdown, with [Sn] citation markers turned into
 * clickable chips. We rewrite [Sn] -> a sentinel markdown link (cite:n) so the
 * markdown parser keeps bold/lists/tables intact AND the citation stays clickable;
 * the Markdown component's link renderer maps cite:n back to onCite(n).
 */
export function MessageContent({ text, onCite }: { text: string; onCite?: (num: number) => void }) {
  const withCites = text.replace(/\[S(\d+)\]/g, (_, n) => `[S${n}](cite:${n})`)
  return <Markdown onCite={onCite}>{withCites}</Markdown>
}
