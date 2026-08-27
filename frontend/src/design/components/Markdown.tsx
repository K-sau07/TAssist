import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

/**
 * Renders AI answer text as Markdown (bold, headings, lists, code, tables, links),
 * styled to the TAssist design tokens — so answers read like ChatGPT/Claude instead
 * of showing raw ** and # characters. Used by the messaging AI margin-note and the
 * private AI chat. Sanitised by react-markdown (no raw HTML executed).
 */
export function Markdown({ children, onCite }: { children: string; onCite?: (num: number) => void }) {
  return (
    <div className="space-y-2 break-words text-md leading-[1.6] text-text">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: ({ children }) => <p className="whitespace-pre-wrap">{children}</p>,
          strong: ({ children }) => <strong className="font-semibold text-text">{children}</strong>,
          em: ({ children }) => <em className="italic">{children}</em>,
          h1: ({ children }) => <h3 className="mt-3 font-display text-lg font-semibold">{children}</h3>,
          h2: ({ children }) => <h3 className="mt-3 font-display text-lg font-semibold">{children}</h3>,
          h3: ({ children }) => <h4 className="mt-2 font-display text-base font-semibold">{children}</h4>,
          h4: ({ children }) => <h4 className="mt-2 font-semibold">{children}</h4>,
          ul: ({ children }) => <ul className="ml-5 list-disc space-y-1">{children}</ul>,
          ol: ({ children }) => <ol className="ml-5 list-decimal space-y-1">{children}</ol>,
          li: ({ children }) => <li className="pl-1">{children}</li>,
          a: ({ children, href }) => {
            // Sentinel citation links (cite:n) render as clickable chips.
            const cite = href?.match(/^cite:(\d+)$/)
            if (cite && onCite) {
              const num = Number(cite[1])
              return (
                <button onClick={() => onCite(num)}
                  className="mx-0.5 inline-flex h-5 items-center rounded-round bg-primary/12 px-1.5 align-middle text-xs font-medium text-primary hover:bg-primary/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus">
                  S{num}
                </button>
              )
            }
            return (
              <a href={href} target="_blank" rel="noopener noreferrer"
                className="text-primary underline underline-offset-2 hover:opacity-80">{children}</a>
            )
          },
          code: ({ className, children }) => {
            const block = /language-/.test(className ?? '')
            return block
              ? <code className="block overflow-x-auto rounded-md bg-bg-sunken p-3 font-mono text-sm">{children}</code>
              : <code className="rounded bg-bg-sunken px-1 py-0.5 font-mono text-[0.85em]">{children}</code>
          },
          pre: ({ children }) => <pre className="overflow-x-auto">{children}</pre>,
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-border pl-3 text-text-muted">{children}</blockquote>
          ),
          table: ({ children }) => (
            <div className="overflow-x-auto"><table className="w-full border-collapse text-sm">{children}</table></div>
          ),
          th: ({ children }) => <th className="border border-border bg-bg-sunken px-2 py-1 text-left font-semibold">{children}</th>,
          td: ({ children }) => <td className="border border-border px-2 py-1">{children}</td>,
          hr: () => <hr className="border-border" />,
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  )
}
