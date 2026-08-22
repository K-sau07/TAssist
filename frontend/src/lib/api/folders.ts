import { apiFetch } from './client'
import type { FileView } from './files'

export interface FolderView { id: string; name: string; createdAt: string; updatedAt: string }

export function listFolders() { return apiFetch<FolderView[]>('/folders') }
export function createFolder(name: string) {
  return apiFetch<FolderView>('/folders', { method: 'POST', body: { name } })
}
export function renameFolder(folderId: string, name: string) {
  return apiFetch<FolderView>(`/folders/${folderId}`, { method: 'PATCH', body: { name } })
}
export function deleteFolder(folderId: string) {
  return apiFetch<void>(`/folders/${folderId}`, { method: 'DELETE' })
}
export function listFolderFiles(folderId: string) {
  return apiFetch<FileView[]>(`/folders/${folderId}/files`)
}
export function addFilesToFolder(folderId: string, fileIds: string[]) {
  return apiFetch<void>(`/folders/${folderId}/files`, { method: 'POST', body: { fileIds } })
}
export function removeFileFromFolder(folderId: string, fileId: string) {
  return apiFetch<void>(`/folders/${folderId}/files/${fileId}`, { method: 'DELETE' })
}
