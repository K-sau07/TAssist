import { useState } from 'react'
import { CheckSquare, Square, Plus, X, ListTodo } from 'lucide-react'
import { useTodosQuery, useCreateTodoMutation, useUpdateTodoMutation, useDeleteTodoMutation } from '@/lib/hooks/useWidgets'

export function TodosCard() {
  const { data: todos = [] } = useTodosQuery()
  const create = useCreateTodoMutation()
  const update = useUpdateTodoMutation()
  const del = useDeleteTodoMutation()
  const [text, setText] = useState('')

  function add() {
    const t = text.trim()
    if (!t) return
    create.mutate(t)
    setText('')
  }

  return (
    <div className="rounded-lg border border-border bg-bg-elev p-5 shadow-1">
      <h3 className="mb-3 flex items-center gap-2 font-medium"><ListTodo size={16} strokeWidth={1.75} /> To-do</h3>

      <div className="mb-3 flex items-center gap-2">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') add() }}
          placeholder="Add a task…"
          className="h-9 flex-1 rounded-md border border-border bg-bg px-3 text-sm outline-none focus:border-primary"
        />
        <button onClick={add} className="grid h-9 w-9 place-items-center rounded-md bg-primary text-primary-fg disabled:opacity-40" disabled={!text.trim()}>
          <Plus size={18} strokeWidth={1.75} />
        </button>
      </div>

      <div className="space-y-1">
        {todos.length === 0 && <p className="text-sm text-text-faint">Nothing yet — add your first task.</p>}
        {todos.map((t) => (
          <div key={t.id} className="group flex items-center gap-2 rounded-md px-1 py-1 hover:bg-bg-sunken">
            <button onClick={() => update.mutate({ id: t.id, patch: { done: !t.done } })} className="text-primary">
              {t.done ? <CheckSquare size={18} strokeWidth={1.75} /> : <Square size={18} strokeWidth={1.75} className="text-text-faint" />}
            </button>
            <span className={`flex-1 text-sm ${t.done ? 'text-text-faint line-through' : ''}`}>{t.text}</span>
            <button onClick={() => del.mutate(t.id)} className="text-text-faint opacity-0 transition-opacity group-hover:opacity-100 hover:text-danger">
              <X size={15} strokeWidth={1.75} />
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
