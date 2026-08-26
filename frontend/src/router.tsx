import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from '@/lib/auth/guard'

import LandingPage from '@/features/landing/LandingPage'
import LoginPage from '@/features/auth/LoginPage'
import SignupPage from '@/features/auth/SignupPage'
import OAuthCompletePage from '@/features/auth/OAuthCompletePage'
import Dashboard from '@/features/dashboard/Dashboard'
import FolderPage from '@/features/folders/FolderPage'
import ChatPage from '@/features/chat/ChatPage'
import NewChatPage from '@/features/chat/NewChatPage'
import MyChannelsPage from '@/features/channels/mine/MyChannelsPage'
import CreateChannelPage from '@/features/channels/mine/CreateChannelPage'
import ChannelManagePage from '@/features/channels/mine/ChannelManagePage'
import DiscoverChannelsPage from '@/features/channels/discover/DiscoverChannelsPage'
import ChannelAboutPage from '@/features/channels/view/ChannelAboutPage'
import ChannelChatPage from '@/features/channels/view/ChannelChatPage'
import ChannelFilesPage from '@/features/channels/view/ChannelFilesPage'
import ChannelShell from '@/features/channels/shell/ChannelShell'
import ChannelIndex from '@/features/channels/shell/ChannelIndex'
import ThreadPage from '@/features/messaging/ThreadPage'
import SettingsPage from '@/features/settings/SettingsPage'
import NotFoundPage from '@/features/misc/NotFoundPage'

const protect = (el: React.ReactNode) => <RequireAuth>{el}</RequireAuth>

// Legacy redirect: old owner manage path -> in-shell manage (D-CS2).
function ManageRedirect() {
  return <Navigate to="/app/channels" replace />
}

// Full route table. 🔒 routes wrapped in <RequireAuth>.
// Channel shell (03_CHANNEL_SHELL_SPEC §4): /c/:handle is a nested layout.
export const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/signup', element: <SignupPage /> },
  { path: '/auth/complete', element: <OAuthCompletePage /> },

  { path: '/app', element: protect(<Dashboard />) },
  { path: '/app/folders/:folderId', element: protect(<FolderPage />) },
  { path: '/app/chats/new', element: protect(<NewChatPage />) },
  { path: '/app/chats/:chatId', element: protect(<ChatPage />) },
  { path: '/app/channels', element: protect(<MyChannelsPage />) },
  { path: '/app/channels/new', element: protect(<CreateChannelPage />) },
  // Legacy manage path -> redirect (D-CS2). Owners now manage inside the shell.
  { path: '/app/channels/:channelId/manage', element: protect(<ManageRedirect />) },
  { path: '/app/discover', element: protect(<DiscoverChannelsPage />) },
  { path: '/app/settings', element: protect(<SettingsPage />) },

  // Public channel page for non-participants (outside the shell).
  { path: '/c/:handle/about', element: protect(<ChannelAboutPage />) },

  // Unified channel shell — participants only (shell redirects others to /about).
  {
    path: '/c/:handle',
    element: protect(<ChannelShell />),
    children: [
      { index: true, element: <ChannelIndex /> },
      { path: 'messages/:conversationId', element: <ThreadPage /> },
      { path: 'chat', element: <ChannelChatPage /> },
      { path: 'chat/:chatId', element: <ChannelChatPage /> },
      { path: 'files', element: <ChannelFilesPage /> },
      { path: 'manage', element: <ChannelManagePage /> },
    ],
  },

  { path: '*', element: <NotFoundPage /> },
])
