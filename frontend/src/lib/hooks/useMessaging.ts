import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  listParticipants, openDm, listMyDms, getGroup,
  listMessages, postMessage, markRead, deleteMessage,
} from '@/lib/api/messaging'

// Query keys are colocated so components + SSE handlers invalidate consistently.
export const msgKeys = {
  participants: (c: string) => ['channel', c, 'participants'] as const,
  dms: (c: string) => ['channel', c, 'dms'] as const,
  group: (c: string) => ['channel', c, 'group'] as const,
  messages: (c: string, conv: string) => ['channel', c, 'conversation', conv, 'messages'] as const,
}

export function useParticipantsQuery(channelId: string, enabled = true) {
  return useQuery({
    queryKey: msgKeys.participants(channelId),
    queryFn: () => listParticipants(channelId),
    enabled: Boolean(channelId) && enabled,
  })
}

export function useMyDmsQuery(channelId: string, enabled = true) {
  return useQuery({
    queryKey: msgKeys.dms(channelId),
    queryFn: () => listMyDms(channelId),
    enabled: Boolean(channelId) && enabled,
  })
}

export function useGroupQuery(channelId: string, enabled = true) {
  return useQuery({
    queryKey: msgKeys.group(channelId),
    queryFn: () => getGroup(channelId),
    enabled: Boolean(channelId) && enabled,
    // group may be 403 (disabled) — don't hammer retries on an expected forbidden
    retry: false,
  })
}

export function useMessagesQuery(channelId: string, conversationId: string, enabled = true) {
  return useQuery({
    queryKey: msgKeys.messages(channelId, conversationId),
    queryFn: () => listMessages(channelId, conversationId, { limit: 50 }),
    enabled: Boolean(channelId) && Boolean(conversationId) && enabled,
  })
}

export function useOpenDmMutation(channelId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (targetUserId: string) => openDm(channelId, targetUserId),
    onSuccess: () => qc.invalidateQueries({ queryKey: msgKeys.dms(channelId) }),
  })
}

export function usePostMessageMutation(channelId: string, conversationId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (content: string) => postMessage(channelId, conversationId, content),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: msgKeys.messages(channelId, conversationId) })
      qc.invalidateQueries({ queryKey: msgKeys.dms(channelId) })
    },
  })
}

export function useDeleteMessageMutation(channelId: string, conversationId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (messageId: string) => deleteMessage(channelId, conversationId, messageId),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: msgKeys.messages(channelId, conversationId) }),
  })
}

export function useMarkReadMutation(channelId: string, conversationId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (upTo?: string) => markRead(channelId, conversationId, upTo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: msgKeys.dms(channelId) })
      qc.invalidateQueries({ queryKey: msgKeys.group(channelId) })
    },
  })
}
