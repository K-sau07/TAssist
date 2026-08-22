import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  listFolders, createFolder, renameFolder, deleteFolder,
  listFolderFiles, addFilesToFolder, removeFileFromFolder, type FolderView,
} from '@/lib/api/folders'

const FOLDERS_KEY = ['folders'] as const

export function useFoldersQuery() {
  return useQuery({ queryKey: FOLDERS_KEY, queryFn: listFolders })
}
export function useFolderFilesQuery(folderId: string) {
  return useQuery({ queryKey: ['folders', folderId, 'files'], queryFn: () => listFolderFiles(folderId) })
}
export function useCreateFolderMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => createFolder(name),
    onSuccess: () => qc.invalidateQueries({ queryKey: FOLDERS_KEY }),
  })
}
export function useRenameFolderMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { folderId: string; name: string }) => renameFolder(v.folderId, v.name),
    onSuccess: () => qc.invalidateQueries({ queryKey: FOLDERS_KEY }),
  })
}
export function useDeleteFolderMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (folderId: string) => deleteFolder(folderId),
    onSuccess: () => qc.invalidateQueries({ queryKey: FOLDERS_KEY }),
  })
}
export function useAddFilesToFolderMutation(folderId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (fileIds: string[]) => addFilesToFolder(folderId, fileIds),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders', folderId, 'files'] }),
  })
}
export function useRemoveFileFromFolderMutation(folderId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (fileId: string) => removeFileFromFolder(folderId, fileId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders', folderId, 'files'] }),
  })
}
export type { FolderView }
