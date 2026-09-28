// Quota API (spec §16.2). Matches backend QuotaController.QuotaResponse.
import { apiFetch } from './client'

export interface QuotaMetric {
  used: number
  limit: number
}

export interface QuotaResponse {
  /** billing period, e.g. "2026-09" */
  period: string
  questions: QuotaMetric
  files: QuotaMetric
  bytesStored: QuotaMetric
  tokens: QuotaMetric
}

export function getQuota() {
  return apiFetch<QuotaResponse>('/quota')
}
