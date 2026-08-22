// SSE event payloads — exact match to backend ChatStreamService.sink.emit(...) (Step 11/12).
export interface SourceItem {
  num: number
  fileId: string | null
  label: string
  similarity: number | null
  snippet: string
}
export interface StartEvent { messageId: string; mode: 'grounded' | 'fallback' | 'regular' | 'spreadsheet' }
export interface SourcesEvent { sources: SourceItem[] }
export interface TokenEvent { text: string }
export interface ToolUseEvent { toolCallId: string; name: string; input: Record<string, unknown> }
export interface ToolResultEvent { toolCallId: string; result: Record<string, unknown> }
export interface CitationEvent { num: number; spanStart: number; spanEnd: number }
export interface DoneEvent { messageId: string; totalInputTokens?: number; totalOutputTokens?: number }
export interface StreamErrorEvent { code: string; message: string }

export interface StreamHandlers {
  onStart?: (d: StartEvent) => void
  onSources?: (d: SourcesEvent) => void
  onToken?: (d: TokenEvent) => void
  onToolUse?: (d: ToolUseEvent) => void
  onToolResult?: (d: ToolResultEvent) => void
  onCitation?: (d: CitationEvent) => void
  onDone?: (d: DoneEvent) => void
  onError?: (d: StreamErrorEvent) => void
}
