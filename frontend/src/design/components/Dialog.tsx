import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import { Button } from './Button'
import { Input } from './Input'
import { AlertTriangle } from 'lucide-react'

interface ConfirmOpts {
  title: string
  message?: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
}
interface PromptOpts {
  title: string
  message?: string
  placeholder?: string
  defaultValue?: string
  confirmLabel?: string
}

interface DialogApi {
  confirm: (opts: ConfirmOpts) => Promise<boolean>
  prompt: (opts: PromptOpts) => Promise<string | null>
}

const DialogContext = createContext<DialogApi | null>(null)

export function useDialog(): DialogApi {
  const ctx = useContext(DialogContext)
  if (!ctx) throw new Error('useDialog must be used within <DialogProvider>')
  return ctx
}

type State =
  | { kind: 'confirm'; opts: ConfirmOpts; resolve: (v: boolean) => void }
  | { kind: 'prompt'; opts: PromptOpts; resolve: (v: string | null) => void }
  | null

export function DialogProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<State>(null)
  const [value, setValue] = useState('')

  const confirm = useCallback((opts: ConfirmOpts) =>
    new Promise<boolean>((resolve) => setState({ kind: 'confirm', opts, resolve })), [])

  const prompt = useCallback((opts: PromptOpts) =>
    new Promise<string | null>((resolve) => {
      setValue(opts.defaultValue ?? '')
      setState({ kind: 'prompt', opts, resolve })
    }), [])

  function close(result: boolean | string | null) {
    if (!state) return
    if (state.kind === 'confirm') state.resolve(result as boolean)
    else state.resolve(result as string | null)
    setState(null)
  }

  return (
    <DialogContext.Provider value={{ confirm, prompt }}>
      {children}
      {state && (
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4"
             role="dialog" aria-modal="true" aria-label={state.opts.title}
             onClick={() => close(state.kind === 'confirm' ? false : null)}
             onKeyDown={(e) => {
               if (e.key === 'Escape') close(state.kind === 'confirm' ? false : null)
               if (e.key === 'Enter' && state.kind === 'prompt') close(value.trim() || null)
             }}
             tabIndex={-1}>
          {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions */}
          <div className="w-full max-w-sm rounded-lg border border-border bg-bg-elev p-6 shadow-2"
               role="document" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-start gap-3">
              {state.kind === 'confirm' && state.opts.danger && (
                <div className="grid h-9 w-9 shrink-0 place-items-center rounded-round bg-danger/15 text-danger">
                  <AlertTriangle size={18} strokeWidth={2} />
                </div>
              )}
              <div className="min-w-0 flex-1">
                <h2 className="text-lg font-medium">{state.opts.title}</h2>
                {state.opts.message && <p className="mt-1 text-sm text-text-muted">{state.opts.message}</p>}

                {state.kind === 'prompt' && (
                  <Input
                    // eslint-disable-next-line jsx-a11y/no-autofocus
                    autoFocus
                    className="mt-4"
                    value={value}
                    placeholder={state.opts.placeholder}
                    onChange={(e) => setValue(e.target.value)}
                  />
                )}
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <Button variant="ghost" onClick={() => close(state.kind === 'confirm' ? false : null)}>
                {state.kind === 'confirm' ? (state.opts.cancelLabel ?? 'Cancel') : 'Cancel'}
              </Button>
              {state.kind === 'confirm' ? (
                <Button variant={state.opts.danger ? 'danger' : 'primary'}
                        onClick={() => close(true)}>
                  {state.opts.confirmLabel ?? 'Confirm'}
                </Button>
              ) : (
                <Button onClick={() => close(value.trim() || null)} disabled={!value.trim()}>
                  {state.opts.confirmLabel ?? 'Create'}
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </DialogContext.Provider>
  )
}
