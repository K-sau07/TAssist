import { apiFetch } from './client'

export type FileStatus = 'UPLOADING' | 'PARSING' | 'EMBEDDING' | 'READY' | 'FAILED'

export interface FileView {
  id: string
  originalFilename: string
  type: string
  sizeBytes: number
  status: FileStatus
  failureReason: string | null
  createdAt: string
  updatedAt: string
}
export interface StatusView { id: string; status: FileStatus; failureReason: string | null }

export function listFiles() { return apiFetch<FileView[]>('/files') }

export function getFileStatus(fileId: string) {
  return apiFetch<StatusView>(`/files/${fileId}/status`)
}

export function uploadFile(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return apiFetch<FileView>('/files', { method: 'POST', body: fd })
}

export function deleteFile(fileId: string) {
  return apiFetch<void>(`/files/${fileId}`, { method: 'DELETE' })
}
