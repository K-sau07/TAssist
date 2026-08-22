import { useMemo, useState } from 'react'
import { useFilesQuery } from '@/lib/hooks/useFiles'
import type { FileView } from '@/lib/api/files'

/** Detects an in-progress @token immediately before the caret. */
function activeToken(text: string, caret: number): { query: string; start: number } | null {
  // look back from caret to the nearest '@' that starts a token (preceded by start/space/newline)
  let i = caret - 1
  while (i >= 0) {
    const ch = text[i]
    if (ch === '@') {
      const before = i === 0 ? ' ' : text[i - 1]
      if (/\s|^/.test(before) || i === 0) return { query: text.slice(i + 1, caret), start: i }
      return null
    }
    if (/\s/.test(ch)) return null // hit whitespace before an '@' → not in a mention
    i--
  }
  return null
}

/** Insert @filename (quoted if it has spaces) replacing the active token. */
export function buildMention(name: string): string {
  return /\s/.test(name) ? `@"${name}"` : `@${name}`
}

export function useMentionPicker(text: string) {
  const { data: files = [] } = useFilesQuery()
  const [caret, setCaret] = useState(0)
  const [active, setActive] = useState(0)

  const token = useMemo(() => activeToken(text, caret), [text, caret])

  const matches = useMemo(() => {
    if (!token) return []
    const q = token.query.toLowerCase()
    return files
      .filter((f) => f.status === 'READY' && f.originalFilename.toLowerCase().includes(q))
      .slice(0, 6)
  }, [token, files])

  const open = token !== null && matches.length > 0

  /** Returns the new text + new caret position after inserting the chosen file. */
  function pick(file: FileView): { text: string; caret: number } {
    if (!token) return { text, caret }
    const mention = buildMention(file.originalFilename) + ' '
    const next = text.slice(0, token.start) + mention + text.slice(caret)
    return { text: next, caret: token.start + mention.length }
  }

  return {
    open, matches, active, setActive, setCaret, pick,
    reset: () => setActive(0),
  }
}
