# 03 — Channel Shell & Slack-Style Navigation Spec

**Status:** DRAFT v1 for review · **Extends:** `01_TASSIST_SPEC.md`, `02_MESSAGING_SPEC.md` · **Type:** Frontend navigation refactor (no backend changes)

> **The one thing to keep sacred:** this is a **pure frontend navigation refactor**. Zero backend changes, zero API changes, zero DB changes. Every existing endpoint, every RAG guarantee, every messaging behaviour from `02_MESSAGING_SPEC.md` stays exactly as built. We are only changing *where the doors are* — how a user moves through a channel — not what any screen does once you're on it.

---

## 0. Why this refactor exists

Today, messaging is buried:

- **Member** clicks a channel (sidebar "Joined") → `ChannelLandingPage` (`/c/@handle`): a static page with the channel description, a "New chat" button, an AI-chat list, and a **"Messages" button** that leads elsewhere. Messaging is a *secondary action*.
- **Owner** clicks a channel (sidebar "My channels") → `ChannelManagePage` (`/app/channels/:id/manage`): an admin console (Overview / Files / Members), with messaging bolted on as a **4th tab**. The owner lands in an *admin view*, not a *place*.

This is not Slack. In Slack, **clicking a channel drops you into the room.** Messages are the front door; everything else (files, settings) is secondary. Owner and member are in the *same space*; the owner just has extra powers.

**This refactor makes the channel a unified "place"** — one shell, entered the same way by everyone, with messaging as the default surface and AI-chat / files / manage as siblings.

---

## 1. Scope & non-scope

**In scope (frontend only):**
- A new persistent **channel shell** layout for everything under `/c/:handle/*`.
- A **secondary rail** inside the channel: group room, DM list, "New message", and section links (AI Chat, Files, Manage).
- Landing behaviour: entering a channel drops you into the **group room** by default (or AI-chat if group is disabled).
- Re-pointing all channel entry points (sidebar Joined / My channels, discover) at the unified shell.
- Folding existing channel pages (messaging thread, AI chat, files, manage, request-access) into the shell as content surfaces.

**Explicitly NOT in scope:**
- ❌ Any backend / API / DB change.
- ❌ Any change to messaging *behaviour* (SSE, optimistic send, read receipts, tombstones, @ai) — only where the thread renders.
- ❌ Any change to the RAG / AI-chat *behaviour* — only where it renders.
- ❌ The global app shell (LeftRail, dashboard, library, private chats) — untouched.
- ❌ Phase 2 messaging items (group toggle UI, `/api/me/unread` cross-channel badges, "read by N"). Those remain deferred and are tracked separately.

---

## 2. Locked design decisions

1. **One shell for all participants.** Owner and approved member enter the same `ChannelShell`. The rail shows a "Manage" link only to the owner; nothing else differs by role in navigation.
2. **Group room is the default surface.** `/c/:handle` redirects to the group thread if the group is enabled; if disabled, it redirects to AI Chat. No static landing page for participants.
3. **Non-participants never see the shell.** Anyone who is not owner/approved (none, pending, rejected, left, banned) is redirected to a standalone **About / request-access page** (`/c/:handle/about`) — the only channel page outside the shell.
4. **Two rails, clear hierarchy.** The global `LeftRail` (home, library, channels list, discover) stays on the far left. The `ChannelRail` (this channel's group/DMs/sections) sits second. Content is third. This mirrors Slack's workspace-rail + channel-rail + content layout.
5. **The shell resolves the channel once.** `ChannelShell` fetches channel + membership a single time and shares it via React context (`useChannelContext`). No child page re-fetches channel identity. This removes the redundant `useChannelPublicQuery` calls scattered across pages today.
6. **Sections are siblings, not nested modes.** Group, DMs, AI Chat, Files, Manage are peer surfaces reachable from the rail. Switching sections never unmounts the shell (rail persists; only content swaps).
7. **AI Chat keeps its exact current behaviour**, including the deferred-creation flow (no blank chats) and the sessionStorage first-message handoff. Only its wrapper changes.
8. **URLs are stable and shareable.** Every surface has a real URL (deep-linkable), listed in §4.
9. **No behaviour regressions.** Every messaging invariant from `02_MESSAGING_SPEC.md` §6 remains true. This spec cannot weaken any of them.

---

## 3. Information architecture (the mental model)

```
┌── Global LeftRail ──┬── ChannelRail ─────────┬── Content ──────────────┐
│ TAssist             │ ← Home                  │                         │
│ [New chat]          │ CSYE7230_HELP_DESK      │  (the active surface:   │
│                     │ @csye7230               │   group thread, a DM,   │
│ LIBRARY             │                         │   AI chat, files, or    │
│  folders…           │ # Group           [3]   │   manage)               │
│                     │                         │                         │
│ CHANNELS            │ DIRECT MESSAGES     +   │                         │
│  My channels        │  • Saurabh Kashyap  [1] │                         │
│  Discover           │  • Alice                │                         │
│                     │                         │                         │
│ JOINED              │ CHANNEL                 │                         │
│  CSYE7230_HELP_DESK │  🤖 AI Chat             │                         │
│                     │  📄 Files               │                         │
│                     │  ⚙ Manage (owner only)  │                         │
│ ─────────           │                         │                         │
│ theme / settings /  │                         │                         │
│ logout              │                         │                         │
└─────────────────────┴─────────────────────────┴─────────────────────────┘
```

Entering a channel from the global rail lands on **# Group** (or AI Chat if group disabled). The ChannelRail persists across all sections.

---

## 4. Route map (the contract)

**Standalone (outside the shell) — unchanged app routes:**
```
/app                                  Dashboard (global)
/app/channels                         My channels (list)
/app/channels/new                     Create channel
/app/discover                         Discover
… (all other /app/* unchanged)
```

**Channel shell (new nested layout under /c/:handle):**
```
/c/:handle                            ChannelShell (layout)
  index                        → ChannelIndex  → redirect to group room, else AI chat
  /c/:handle/messages/:conversationId  → ThreadPage      (group or DM, in shell)
  /c/:handle/chat                      → ChannelChatPage (AI chat home / new)
  /c/:handle/chat/:chatId              → ChannelChatPage (a specific AI chat)
  /c/:handle/files                     → ChannelFilesPage (read-only file list for members;
                                          owner sees the same, manages via Manage)
  /c/:handle/manage                    → ChannelManagePage (owner only; Overview/Files/Members)
```

**Standalone channel page (outside shell):**
```
/c/:handle/about                      → ChannelAboutPage (description + request access;
                                         for non-participants; shell redirects here)
```

**Route changes / migrations:**
- OLD `/c/:handle` (ChannelLandingPage) → SPLIT into `ChannelIndex` (participants, in shell) + `ChannelAboutPage` (non-participants, standalone). `ChannelLandingPage.tsx` is retired.
- OLD `/c/:handle/chats/:chatId` → RENAMED `/c/:handle/chat/:chatId` (singular, consistent). Add `/c/:handle/chat` for the home/new state. (Old plural path may 301-style redirect for one release, or just be dropped since links are internal.)
- OLD `/c/:handle/messages` (MessagesHomePage standalone) → RETIRED as a route. The messaging "home" is now the group room / rail. `MessagesHome` component logic is absorbed by the rail; the standalone page is removed. (Keep the file only if a "messages overview" surface is still wanted — decision below.)
- OLD `/app/channels/:id/manage` → KEPT for back-compat, but the primary owner path becomes `/c/:handle/manage` inside the shell. Manage renders inside the shell; the standalone `/app/...` route can redirect to `/c/@handle/manage`.

**Open decision D-1 (needs your call):** do we keep a dedicated "Messages overview" surface (inbox-style list of all DMs + group, like the current `MessagesHomePage`), reachable from the rail, in addition to the rail's own list? Slack does *not* (the rail IS the list). Recommendation: **drop it** — the rail is the index. Retire `MessagesHomePage`.

---

## 5. Component contracts

**`ChannelShell.tsx`** (new, layout)
- Reads `:handle`, fetches channel+membership once via `useChannelPublicQuery`.
- States: loading → skeleton with global rail; not-found → message; non-participant → `<Navigate to=/c/:handle/about>`.
- Provides `ChannelContext { channel, username, status, isOwner, canUse }` via `useChannelContext()`.
- Renders: `<LeftRail/>` + `<ChannelRail/>` + `<Outlet/>`.

**`ChannelRail.tsx`** (new)
- Consumes `useChannelContext`, `useMyDmsQuery`, `useGroupQuery`.
- Renders: back-to-home link, channel name/@handle, `# Group` (with unread badge), DM list (with unread badges + "New message" +), and section links (AI Chat, Files, Manage[owner]).
- Owns the `NewMessagePicker` modal trigger.
- Active-state highlighting via `NavLink`.

**`ChannelIndex.tsx`** (new)
- Consumes context + `useGroupQuery`; redirects to group thread or AI chat. No UI beyond a loading state.

**`ChannelAboutPage.tsx`** (new, standalone — extracted from old ChannelLandingPage)
- The non-participant view: channel description, "what to expect", and the request-access / pending / banned states. Wrapped in `AppLayout` (global rail only, no channel rail).

**`NewMessagePicker.tsx`** (new, extracted from MessagesHomePage)
- Shared modal: search participants, open-or-create DM. Already extracted this session.

**Modified to render inside shell (drop `AppLayout`, use `useChannelContext`):**
- `ThreadPage.tsx` — ✅ done this session (uses context, header shows # Group / DM name).
- `ChannelChatPage.tsx` — ✅ done this session (uses context, singular `/chat` path).
- `ChannelManagePage.tsx` — to modify: drop `AppLayout`, read channel from context, keep tabs. The "Messages" tab added earlier is **removed** (messaging now lives in the rail, not a manage tab).
- `ChannelFilesPage.tsx` — new thin wrapper around the existing channel files list for the member-facing read-only view.

**Untouched:** all messaging logic/components (`MessageComposer`, `MessageBubble`, `logic.ts`, SSE, hooks), all AI-chat stream logic, the global `LeftRail`, `AppLayout`.

---

## 6. Invariants (MUST NOT break)

1. **No backend/API/DB change.** If any step needs one, STOP — it's out of scope.
2. **Every `02_MESSAGING_SPEC.md` §6 invariant holds** (RAG grounding, membership gates live, DM freeze on ban, soft-delete, quota, no silent failures).
3. **AI-chat deferred-creation** (no blank chats) + sessionStorage first-message handoff still works.
4. **Non-participants can never reach messaging or AI chat** — the shell redirects them to `/about` before any child renders.
5. **Owner-only "Manage"** link is the only role-conditional nav element.
6. **Deep links work** — pasting any §4 URL lands correctly (or redirects sanely).
7. **No double rails, no double fetches** — the shell renders the global + channel rail exactly once; children never re-wrap `AppLayout` or re-fetch channel identity.
8. **Both test suites stay green**; new pure logic gets Vitest coverage; granular commits per milestone.

---

## 7. Build order (compile-green at every step, one commit each)

- **S1 — Shell scaffolding.** `ChannelShell` + `ChannelContext` + `ChannelRail` + `ChannelIndex` + extracted `NewMessagePicker`. (Rail may link to not-yet-wired routes.) Typecheck green. ✅ *partially done this session — S1 files exist.*
- **S2 — Fold Thread + AI Chat into shell.** Convert `ThreadPage` + `ChannelChatPage` to context/no-AppLayout. ✅ *done this session.*
- **S3 — About page split.** Extract `ChannelAboutPage` from `ChannelLandingPage`; retire the landing page.
- **S4 — Files + Manage in shell.** `ChannelFilesPage` wrapper; convert `ChannelManagePage` to shell (drop AppLayout + Messages tab).
- **S5 — Rewire routes.** Update `router.tsx` to the §4 map (nested shell). Update sidebar links (`LeftRail` Joined → `/c/@handle`; `MyChannelsPage` → `/c/@handle`). Redirect old `/app/channels/:id/manage` and old chat/messages paths.
- **S6 — Cleanup.** Retire `MessagesHomePage` (per D-1), remove the old Manage "Messages" tab file, delete dead imports. Grep for any link to retired routes.
- **S7 — Verify + tests.** Full gauntlet (typecheck/lint/test/build). Add/adjust Vitest for any new pure logic. Manual two-sided smoke.
- **S8 — Commit trail + BUILD_LOG.** Granular commits `S<n>: <what>`, D-numbered decisions appended to BUILD_LOG, push, confirm CI green.

---

## 8. Reconciliation with work already started this session

Before writing this spec I began S1/S2 directly (breaking our spec-first rule — corrected now). Current uncommitted state:
- ✅ `ChannelShell.tsx`, `ChannelRail.tsx`, `ChannelIndex.tsx` created.
- ✅ `NewMessagePicker.tsx` extracted.
- ✅ `ThreadPage.tsx`, `ChannelChatPage.tsx` converted to shell/context.
- ⚠️ Routes NOT yet rewired — so the app currently still uses old routes and these new files are dark. Nothing is broken (old paths untouched), but the new shell is not reachable yet.
- ⚠️ The earlier `MessagesTab` on Manage (committed in `91c36aa`) will be **removed** in S6 per this spec.

**Decision:** this uncommitted work is consistent with the spec above. We proceed from S3, keeping S1/S2 as-is, and treat this spec as the authority from here.

---

## 9. Open decisions for sign-off

- **D-1:** Retire the standalone Messages overview page (rail is the index)? *Recommendation: yes.*
- **D-2:** Keep old `/app/channels/:id/manage` as a redirect to `/c/@handle/manage`, or hard-move? *Recommendation: redirect for safety.*
- **D-3:** Old `/c/:handle/chats/:chatId` (plural) — redirect or drop? Links are internal only. *Recommendation: drop (update all internal links in S5).*
- **D-4:** Files section for members — read-only list now, or defer entirely and only show Files under owner Manage? *Recommendation: read-only member list now (cheap, expected in Slack-like channels).*

---

*End of draft. Nothing gets built past S2 until this spec is signed off and D-1…D-4 are decided.*
