import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createChannel,
  listMyChannels, listChannelFiles, listMembers, attachChannelFile, detachChannelFile,
  approveMember, denyMember, kickMember, banMember,
  type MembershipStatus, type CreateChannelInput,
} from '@/lib/api/channels'

export function useMyChannelsQuery() {
  return useQuery({ queryKey: ['channels', 'mine'], queryFn: listMyChannels })
}
export function useChannelFilesQuery(channelId: string) {
  return useQuery({ queryKey: ['channel', channelId, 'files'], queryFn: () => listChannelFiles(channelId) })
}
export function useMembersQuery(channelId: string, status: MembershipStatus) {
  return useQuery({ queryKey: ['channel', channelId, 'members', status], queryFn: () => listMembers(channelId, status) })
}
export function useAttachFileMutation(channelId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { fileId: string; displayLabel: string }) => attachChannelFile(channelId, v.fileId, v.displayLabel),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channel', channelId, 'files'] }),
  })
}
export function useDetachFileMutation(channelId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (fileId: string) => detachChannelFile(channelId, fileId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channel', channelId, 'files'] }),
  })
}
export function useMemberActionMutation(channelId: string) {
  const qc = useQueryClient()
  const run = (action: 'approve' | 'deny' | 'kick' | 'ban', membershipId: string) => {
    const fn = { approve: approveMember, deny: denyMember, kick: kickMember, ban: banMember }[action]
    return fn(channelId, membershipId)
  }
  return useMutation({
    mutationFn: (v: { action: 'approve' | 'deny' | 'kick' | 'ban'; membershipId: string }) =>
      run(v.action, v.membershipId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channel', channelId, 'members'] }),
  })
}

export function useCreateChannelMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateChannelInput) => createChannel(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channels', 'mine'] }),
  })
}
