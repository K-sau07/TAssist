import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { channelDirectory, getChannelByUsername, requestJoin, leaveChannel } from '@/lib/api/channels'

export function useDirectoryQuery() {
  return useQuery({ queryKey: ['channels', 'directory'], queryFn: () => channelDirectory(0) })
}
export function useChannelPublicQuery(username: string) {
  return useQuery({
    queryKey: ['channel-public', username],
    queryFn: () => getChannelByUsername(username),
    enabled: Boolean(username),
  })
}
export function useRequestJoinMutation(username: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { channelId: string; message?: string }) => requestJoin(v.channelId, v.message),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channel-public', username] }),
  })
}
export function useLeaveChannelMutation(username: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (channelId: string) => leaveChannel(channelId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['channel-public', username] }),
  })
}
