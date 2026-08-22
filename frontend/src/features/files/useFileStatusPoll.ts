import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getFileStatus, type FileStatus } from '@/lib/api/files'

const settled = (s?: FileStatus) => s === 'READY' || s === 'FAILED'

/** Polls a single file's status until it settles, then updates the files list cache. */
export function useFileStatusPoll(fileId: string, initial: FileStatus, enabled: boolean) {
  const qc = useQueryClient()
  return useQuery({
    queryKey: ['file-status', fileId],
    queryFn: async () => {
      const s = await getFileStatus(fileId)
      // reflect into the files list so the card re-renders with the new status
      qc.setQueryData<any[]>(['files'], (old) =>
        old?.map((f) => (f.id === fileId ? { ...f, status: s.status, failureReason: s.failureReason } : f)))
      return s
    },
    enabled: enabled && !settled(initial),
    refetchInterval: (q) => (settled(q.state.data?.status) ? false : 1500),
    initialData: { id: fileId, status: initial, failureReason: null },
  })
}
