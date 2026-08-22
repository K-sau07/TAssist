import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getNote, updateNote, listTodos, createTodo, updateTodo, deleteTodo } from '@/lib/api/widgets'

export function useNoteQuery() { return useQuery({ queryKey: ['note'], queryFn: getNote }) }
export function useUpdateNoteMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (content: string) => updateNote(content),
    onSuccess: (n) => qc.setQueryData(['note'], n),
  })
}
export function useTodosQuery() { return useQuery({ queryKey: ['todos'], queryFn: listTodos }) }
export function useCreateTodoMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (text: string) => createTodo(text),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['todos'] }),
  })
}
export function useUpdateTodoMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { id: string; patch: { text?: string; done?: boolean; position?: number } }) => updateTodo(v.id, v.patch),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['todos'] }),
  })
}
export function useDeleteTodoMutation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteTodo(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['todos'] }),
  })
}
