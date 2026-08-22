import type { ReactNode } from 'react'
import { LeftRail } from './LeftRail'

export function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-bg">
      <LeftRail />
      <div className="flex-1 overflow-y-auto">{children}</div>
    </div>
  )
}
