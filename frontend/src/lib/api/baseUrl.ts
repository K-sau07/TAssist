/**
 * API origin. Empty in development so Vite's proxy handles `/api/...`; in
 * production the UI is on Vercel and the API on Render, so they are separate
 * origins and VITE_API_BASE supplies the backend URL at build time.
 */
const API_BASE = (import.meta.env.VITE_API_BASE ?? '').replace(/\/$/, '');

export function apiUrl(path: string): string {
  return `${API_BASE}/api${path}`;
}
