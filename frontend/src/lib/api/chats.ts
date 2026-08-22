import { apiFetch } from './client'

export interface CitationView { fileId: string; chunkId: string; label: string; snippet: string | null }
export interface MessageView {
  id: string; role: 'USER' | 'ASSISTANT'; content: string
  citations: CitationView[]; mentionedFiles: string[]; createdAt: string
}
export interface ChatView {
  id: string; scope: 'REGULAR' | 'FOLDER' | 'CHANNEL'
  folderId: string | null; title: string; createdAt: string; updatedAt: string
}
export interface ChatDetail { chat: ChatView; messages: MessageView[] }

export function listChats() { return apiFetch<ChatView[]>('/chats') }
export function getChat(chatId: string) { return apiFetch<ChatDetail>(`/chats/${chatId}`) }
export function createChat(scope: 'REGULAR' | 'FOLDER', folderId?: string) {
  return apiFetch<ChatView>('/chats', { method: 'POST', body: { scope, folderId: folderId ?? null } })
}
export function renameChat(chatId: string, title: string) {
  return apiFetch<ChatView>(`/chats/${chatId}`, { method: 'PATCH', body: { title } })
}
export function deleteChat(chatId: string) {
  return apiFetch<void>(`/chats/${chatId}`, { method: 'DELETE' })
}
export const chatStreamPath = (chatId: string) => `/chats/${chatId}/messages/stream`
