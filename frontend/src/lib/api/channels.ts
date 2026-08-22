import { apiFetch } from './client'

export type Visibility = 'PUBLIC' | 'UNLISTED' | 'PRIVATE'
export type MembershipStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'BANNED' | 'LEFT'

export interface ChannelView {
  id: string; username: string; displayName: string; description: string
  expectationSummary: string; visibility: Visibility; createdAt: string
}
export interface ChannelPublicView { channel: ChannelView; myMembershipStatus: MembershipStatus | 'OWNER' | null }
export interface ChannelFileView { fileId: string; displayLabel: string; addedAt: string }
export interface MembershipView {
  id: string; userId: string
  displayName: string | null; email: string | null
  status: MembershipStatus; requestMessage: string | null; createdAt: string
}

export interface CreateChannelInput {
  username: string; displayName: string; description?: string
  expectationSummary?: string; visibility: Visibility; requireMessageOnReRequest?: boolean
}

// --- channel CRUD ---
export function listMyChannels() { return apiFetch<ChannelView[]>('/channels/mine') }
export function getChannelByUsername(username: string) {
  return apiFetch<ChannelPublicView>(`/channels/@${username}`)
}
export function createChannel(input: CreateChannelInput) {
  return apiFetch<ChannelView>('/channels', { method: 'POST', body: input })
}
export function editChannel(channelId: string, patch: Partial<CreateChannelInput>) {
  return apiFetch<ChannelView>(`/channels/${channelId}`, { method: 'PATCH', body: patch })
}
export function deleteChannel(channelId: string) {
  return apiFetch<void>(`/channels/${channelId}`, { method: 'DELETE' })
}
export function searchChannels(q: string) {
  return apiFetch<ChannelView[]>(`/channels/search?q=${encodeURIComponent(q)}`)
}
export function channelDirectory(page = 0) {
  return apiFetch<ChannelView[]>(`/channels/directory?page=${page}`)
}

// --- channel files ---
export function listChannelFiles(channelId: string) {
  return apiFetch<ChannelFileView[]>(`/channels/${channelId}/files`)
}
export function attachChannelFile(channelId: string, fileId: string, displayLabel: string) {
  return apiFetch<ChannelFileView>(`/channels/${channelId}/files`, {
    method: 'POST', body: { fileId, displayLabel },
  })
}
export function renameChannelFileLabel(channelId: string, fileId: string, displayLabel: string) {
  return apiFetch<ChannelFileView>(`/channels/${channelId}/files/${fileId}`, {
    method: 'PATCH', body: { displayLabel },
  })
}
export function detachChannelFile(channelId: string, fileId: string) {
  return apiFetch<void>(`/channels/${channelId}/files/${fileId}`, { method: 'DELETE' })
}

// --- membership (owner side) ---
export function listMembers(channelId: string, status: MembershipStatus) {
  return apiFetch<MembershipView[]>(`/channels/${channelId}/members?status=${status}`)
}
const memberAction = (verb: string) => (channelId: string, membershipId: string) =>
  apiFetch<MembershipView>(`/channels/${channelId}/members/${membershipId}/${verb}`, { method: 'POST' })
export const approveMember = memberAction('approve')
export const denyMember = memberAction('deny')
export const kickMember = memberAction('kick')
export const banMember = memberAction('ban')

// --- membership (visitor side) ---
export function requestJoin(channelId: string, message?: string) {
  return apiFetch<MembershipView>(`/channels/${channelId}/join`, { method: 'POST', body: { message: message ?? null } })
}
export function leaveChannel(channelId: string) {
  return apiFetch<void>(`/channels/${channelId}/membership`, { method: 'DELETE' })
}
