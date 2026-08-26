import { Navigate } from 'react-router-dom'
import { useChannelContext } from './ChannelShell'
import { useGroupQuery } from '@/lib/hooks/useMessaging'

/**
 * Default channel surface (/c/:handle). Slack-style: land in the group room if it
 * exists, otherwise fall back to the AI-chat home. Keeps the "channel = a place you
 * enter" feel rather than a static landing page.
 */
export default function ChannelIndex() {
  const { username } = useChannelContext()
  const group = useGroupQuery(useChannelContext().channel.id)

  if (group.isLoading) {
    return <div className="grid h-screen place-items-center text-text-muted">Loading…</div>
  }
  if (group.data) {
    return <Navigate to={`/c/@${username}/messages/${group.data.id}`} replace />
  }
  // group disabled → default to AI chat home
  return <Navigate to={`/c/@${username}/chat`} replace />
}
