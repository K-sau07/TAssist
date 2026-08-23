import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { queryClient } from '@/lib/queryClient'
import { router } from '@/router'
import { DialogProvider } from '@/design/components/Dialog'

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <DialogProvider>
        <RouterProvider router={router} />
      </DialogProvider>
    </QueryClientProvider>
  )
}
