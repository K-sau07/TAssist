// Channel messaging API client (02_MESSAGING_SPEC §9). DM + group conversations,
// messages, read state. Mirrors the M6 REST contract exactly.
import { apiFetch } from './client'

export type ConversationKind = 'DM' | 'GROUP'
export type MessageSenderKind = 'HUMAN' | 'AI'

export interface ParticipantView {
  userId: string
  displayName: string
  isOwner: boolean
}

export interface ConversationView {
  id: string
  channelId: string
  kind: ConversationKind
  otherParticipant: ParticipantView | null // null for GROUP
  lastMessagePreview: string | null
  unreadCount: number
  updatedAt: string
}

export interface CitationView {
  fileId: string
  chunkId: string
  displayLabel: string
  snippet: string | null
}

export interface MessageSender {
  userId: string | null
  displayName: string
}

export interface MessageView {
  id: string
  senderKind: MessageSenderKind
  sender: MessageSender | null // null for AI
  content: string | null // null when deleted (tombstone)
  citations: CitationView[]
  createdAt: string
  deleted: boolean
}

export interface PostMessageResponse {
  message: MessageView
  aiReply: MessageView | null
}

// ── participants + DMs ──
export function listParticipants(channelId: string) {
  return apiFetch<ParticipantView[]>(`/channels/${channelId}/participants`)
}
export function openDm(channelId: string, targetUserId: string) {
  return apiFetch<ConversationView>(`/channels/${channelId}/dm`, {
    method: 'POST',
    body: { targetUserId },
  })
}
export function listMyDms(channelId: string) {
  return apiFetch<ConversationView[]>(`/channels/${channelId}/dms`)
}

// ── group ──
export function getGroup(channelId: string) {
  return apiFetch<ConversationView>(`/channels/${channelId}/group`)
}
export function setGroupEnabled(channelId: string, enabled: boolean) {
  return apiFetch<void>(`/channels/${channelId}/group/enabled`, {
    method: 'PUT',
    body: { enabled },
  })
}

// ── shared conversation ops (DM or GROUP) ──
export function listMessages(
  channelId: string,
  conversationId: string,
  opts?: { before?: string; limit?: number },
) {
  const q = new URLSearchParams()
  if (opts?.before) q.set('before', opts.before)
  if (opts?.limit) q.set('limit', String(opts.limit))
  const qs = q.toString()
  return apiFetch<MessageView[]>(
    `/channels/${channelId}/conversations/${conversationId}/messages${qs ? `?${qs}` : ''}`,
  )
}
export function postMessage(channelId: string, conversationId: string, content: string) {
  return apiFetch<PostMessageResponse>(
    `/channels/${channelId}/conversations/${conversationId}/messages`,
    { method: 'POST', body: { content } },
  )
}
export function markRead(channelId: string, conversationId: string, upTo?: string) {
  return apiFetch<void>(
    `/channels/${channelId}/conversations/${conversationId}/read`,
    { method: 'POST', body: upTo ? { upTo } : {} },
  )
}
export function deleteMessage(channelId: string, conversationId: string, messageId: string) {
  return apiFetch<void>(
    `/channels/${channelId}/conversations/${conversationId}/messages/${messageId}`,
    { method: 'DELETE' },
  )
}
export const conversationStreamPath = (channelId: string, conversationId: string) =>
  `/channels/${channelId}/conversations/${conversationId}/stream`
