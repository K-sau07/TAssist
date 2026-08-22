import { apiFetch } from './client'

export interface NoteView { id: string; content: string; updatedAt: string }
export interface TodoView { id: string; text: string; done: boolean; position: number; updatedAt: string }

export function getNote() { return apiFetch<NoteView>('/notes') }
export function updateNote(content: string) {
  return apiFetch<NoteView>('/notes', { method: 'PUT', body: { content } })
}
export function listTodos() { return apiFetch<TodoView[]>('/todos') }
export function createTodo(text: string) {
  return apiFetch<TodoView>('/todos', { method: 'POST', body: { text } })
}
export function updateTodo(id: string, patch: { text?: string; done?: boolean; position?: number }) {
  return apiFetch<TodoView>(`/todos/${id}`, { method: 'PATCH', body: patch })
}
export function deleteTodo(id: string) {
  return apiFetch<void>(`/todos/${id}`, { method: 'DELETE' })
}
