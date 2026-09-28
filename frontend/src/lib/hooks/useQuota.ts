import { useQuery } from '@tanstack/react-query'
import { getQuota, type QuotaResponse } from '@/lib/api/quota'

const QUOTA_KEY = ['quota'] as const

export function useQuotaQuery() {
  return useQuery({ queryKey: QUOTA_KEY, queryFn: getQuota })
}

export type { QuotaResponse }
