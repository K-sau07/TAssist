// De-dupe citations for display (05_GLOWUP; retrieval Phase B finding).
// Multiple citations can point to the same file (distinct chunks, same displayLabel).
// The sources strip should show each unique source ONCE — like ChatGPT/Claude — rather
// than repeating the filename. We keep the first citation per label (its snippet opens
// on click) and preserve first-seen order.

export interface LabeledCitation {
  fileId: string
  chunkId: string
  displayLabel: string
  snippet: string | null
}

/** One entry per unique displayLabel, first occurrence kept, original order preserved. */
export function dedupeCitations<T extends LabeledCitation>(citations: T[]): T[] {
  const seen = new Set<string>()
  const out: T[] = []
  for (const c of citations) {
    if (seen.has(c.displayLabel)) continue
    seen.add(c.displayLabel)
    out.push(c)
  }
  return out
}
