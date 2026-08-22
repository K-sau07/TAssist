import { apiFetch } from './client'

export interface ChannelChatSummary { id: string; channelId: string; title: string; createdAt: string }
export interface ChannelMsg { id: string; role: 'USER' | 'ASSISTANT'; content: string; createdAt: string }
export interface ChannelChatWithMessages { chat: ChannelChatSummary; messages: ChannelMsg[] }

export function listChannelChats(channelId: string) {
  return apiFetch<ChannelChatSummary[]>(`/channels/${channelId}/chats`)
}
export function createChannelChat(channelId: string) {
  return apiFetch<ChannelChatSummary>(`/channels/${channelId}/chats`, { method: 'POST' })
}
export function getChannelChat(channelId: string, chatId: string) {
  return apiFetch<ChannelChatWithMessages>(`/channels/${channelId}/chats/${chatId}`)
}
export const channelChatStreamPath = (channelId: string, chatId: string) =>
  `/channels/${channelId}/chats/${chatId}/messages/stream`
