import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listFiles, uploadFile, deleteFile, type FileView } from '@/lib/api/files'

const FILES_KEY = ['files'] as const

export function useFilesQuery() {
  return useQuery({ queryKey: FILES_KEY, queryFn: listFiles })
}

export function useUploadFileMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => uploadFile(file),
    onSuccess: () => { qc.invalidateQueries({ queryKey: FILES_KEY }) },
  })
}

export function useDeleteFileMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (fileId: string) => deleteFile(fileId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: FILES_KEY }) },
  })
}

export type { FileView }
