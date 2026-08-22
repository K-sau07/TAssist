import { createBrowserRouter } from 'react-router-dom'
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
import ChannelLandingPage from '@/features/channels/view/ChannelLandingPage'
import ChannelChatPage from '@/features/channels/view/ChannelChatPage'
import SettingsPage from '@/features/settings/SettingsPage'
import NotFoundPage from '@/features/misc/NotFoundPage'

const protect = (el: React.ReactNode) => <RequireAuth>{el}</RequireAuth>

// Full route table (spec §13.2). 🔒 routes wrapped in <RequireAuth>.
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
  { path: '/app/channels/:channelId/manage', element: protect(<ChannelManagePage />) },
  { path: '/app/discover', element: protect(<DiscoverChannelsPage />) },
  { path: '/app/settings', element: protect(<SettingsPage />) },

  { path: '/c/:handle', element: protect(<ChannelLandingPage />) },
  { path: '/c/:handle/chats/:chatId', element: protect(<ChannelChatPage />) },

  { path: '*', element: <NotFoundPage /> },
])
