import { NavLink, useNavigate } from 'react-router-dom'
import { FilePlus2, FolderPlus, Compass, Settings, LogOut, Hash, Library } from 'lucide-react'
import { Button } from '@/design/components/Button'
import { useFoldersQuery, useCreateFolderMutation } from '@/lib/hooks/useFolders'
import { useJoinedChannelsQuery } from '@/lib/hooks/useChannels'
import { useAuthStore } from '@/lib/auth/store'
import { cn } from '@/lib/cn'

export function LeftRail() {
  const navigate = useNavigate()
  const { data: folders = [] } = useFoldersQuery()
  const createFolder = useCreateFolderMutation()
  const { data: joined = [] } = useJoinedChannelsQuery()
  const clear = useAuthStore((s) => s.clear)

  function newFolder() {
    const name = window.prompt('Folder name')?.trim()
    if (name) createFolder.mutate(name)
  }
  function logout() { clear(); navigate('/login', { replace: true }) }

  const linkCls = ({ isActive }: { isActive: boolean }) =>
    cn('flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors',
       isActive ? 'bg-bg-sunken text-text font-medium' : 'text-text-muted hover:bg-bg-sunken')

  return (
    <aside className="flex h-screen w-[240px] shrink-0 flex-col border-r border-border bg-bg-elev px-3 py-4">
      <NavLink to="/app" className="px-3 font-display text-xl text-primary">TAssist</NavLink>

      <div className="mt-4">
        <Button className="w-full" onClick={() => navigate('/app/chats/new')}>
          <FilePlus2 size={18} strokeWidth={1.75} /> New chat
        </Button>
      </div>

      <nav className="mt-6 flex-1 overflow-y-auto">
        <div className="mb-1 flex items-center justify-between px-3">
          <span className="flex items-center gap-1.5 text-xs uppercase tracking-wider text-text-faint">
            <Library size={13} strokeWidth={1.75} /> Library
          </span>
          <button onClick={newFolder} className="text-text-faint hover:text-primary" title="New folder">
            <FolderPlus size={15} strokeWidth={1.75} />
          </button>
        </div>
        <div className="mb-4">
          {folders.length === 0 && <p className="px-3 py-1 text-xs text-text-faint">No folders yet</p>}
          {folders.map((f) => (
            <NavLink key={f.id} to={`/app/folders/${f.id}`} className={linkCls}>
              <Hash size={16} strokeWidth={1.75} /> {f.name}
            </NavLink>
          ))}
        </div>

        <div className="mb-1 px-3 text-xs uppercase tracking-wider text-text-faint">Channels</div>
        <NavLink to="/app/channels" className={linkCls}><Hash size={16} strokeWidth={1.75} /> My channels</NavLink>
        <NavLink to="/app/discover" className={linkCls}><Compass size={16} strokeWidth={1.75} /> Discover</NavLink>

        {joined.length > 0 && (
          <div className="mt-4">
            <div className="mb-1 px-3 text-xs uppercase tracking-wider text-text-faint">Joined</div>
            {joined.map((c) => (
              <NavLink key={c.id} to={`/c/@${c.username}`} className={linkCls} title={c.displayName}>
                <Hash size={16} strokeWidth={1.75} /> <span className="truncate">{c.displayName}</span>
              </NavLink>
            ))}
          </div>
        )}
      </nav>

      <div className="border-t border-border pt-2">
        <NavLink to="/app/settings" className={linkCls}><Settings size={16} strokeWidth={1.75} /> Settings</NavLink>
        <button onClick={logout} className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm text-text-muted hover:bg-bg-sunken">
          <LogOut size={16} strokeWidth={1.75} /> Log out
        </button>
      </div>
    </aside>
  )
}
