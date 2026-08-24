import { MessagesHome } from '@/features/messaging/MessagesHomePage'
import type { ChannelView } from '@/lib/api/channels'

/**
 * Owner's Messages tab on the Manage page. Gives the owner the same messaging
 * space members get (group room + DMs + new message), so the owner is a
 * first-class participant, not just an admin (02_MESSAGING_SPEC §2, §10).
 */
export function MessagesTab({ channel }: { channel: ChannelView }) {
  return (
    <div className="max-w-2xl">
      <MessagesHome channelId={channel.id} username={channel.username} />
    </div>
  )
}
