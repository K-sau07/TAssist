import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Input } from '@/design/components/Input'
import { Button } from '@/design/components/Button'
import { editChannel, deleteChannel, type ChannelView } from '@/lib/api/channels'
import { useQueryClient } from '@tanstack/react-query'

export function OverviewTab({ channel }: { channel: ChannelView }) {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [displayName, setDisplayName] = useState(channel.displayName)
  const [description, setDescription] = useState(channel.description)
  const [expectationSummary, setExpectationSummary] = useState(channel.expectationSummary)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  async function save() {
    setSaving(true); setSaved(false)
    try {
      await editChannel(channel.id, { displayName, description, expectationSummary })
      qc.invalidateQueries({ queryKey: ['channels', 'mine'] })
      setSaved(true)
    } finally { setSaving(false) }
  }
  async function remove() {
    if (!confirm(`Delete @${channel.username}? This removes all files, members, and chats. This cannot be undone.`)) return
    if (!confirm('Are you absolutely sure? This is permanent.')) return
    await deleteChannel(channel.id)
    qc.invalidateQueries({ queryKey: ['channels', 'mine'] })
    navigate('/app/channels')
  }

  return (
    <div className="max-w-xl space-y-5">
      <div>
        <span className="mb-1 block text-sm font-medium">Display name</span>
        <Input value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
      </div>
      <div>
        <span className="mb-1 block text-sm font-medium">Description</span>
        <textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)}
          className="w-full rounded-md border border-border bg-bg-elev px-3 py-2 text-md outline-none focus:border-primary" />
      </div>
      <div>
        <span className="mb-1 block text-sm font-medium">What to expect</span>
        <textarea rows={2} value={expectationSummary} onChange={(e) => setExpectationSummary(e.target.value)}
          className="w-full rounded-md border border-border bg-bg-elev px-3 py-2 text-md outline-none focus:border-primary" />
      </div>
      <div className="flex items-center gap-3">
        <Button onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</Button>
        {saved && <span className="text-sm text-success">Saved</span>}
      </div>

      <div className="mt-10 rounded-lg border border-danger/30 bg-danger/5 p-5">
        <p className="font-medium text-danger">Danger zone</p>
        <p className="mt-1 text-sm text-text-muted">Deleting a channel removes all its files, members, and chats.</p>
        <Button variant="danger" className="mt-3" onClick={remove}>Delete channel</Button>
      </div>
    </div>
  )
}
