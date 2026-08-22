import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        'h-11 w-full rounded-md border border-border bg-bg-elev px-3 text-md text-text',
        'placeholder:text-text-faint',
        'focus-visible:outline-none focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/30',
        'disabled:opacity-50', className,
      )}
      {...props}
    />
  ),
)
Input.displayName = 'Input'
