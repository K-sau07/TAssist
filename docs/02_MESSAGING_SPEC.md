# TAssist — Channel Collaboration Spec (Messaging + Community Layer)

**Status:** DRAFT v2 for review · **Extends:** `01_TASSIST_SPEC.md` · **Companion to:** the existing RAG/channel system

> **The one thing to keep sacred:** TAssist's agenda is **RAG-grounded AI answers over the owner's documents, with zero raw-file exposure and no hallucination.** Everything below is a *collaboration layer on top of that*. Messaging makes a channel a living space (TA <-> students, students <-> students), but the **grounded AI over course material remains the product's soul.** If any messaging feature ever competes with or dilutes that, the AI wins.

---

## 0. How this reconciles with the base spec

`01_TASSIST_SPEC.md` section 2 lists a Phase-1 non-goal: *"Not a real-time collaboration tool. No live co-editing, no comments on docs."* This spec **consciously graduates that boundary** for a Phase-2 capability, with two guardrails:
1. It is **additive** -- new tables, new endpoints, new UI. The existing private library, AI chat, RAG pipeline, and channel Q&A are untouched.
2. It stays **document-centric** -- the collaboration exists *around the knowledge*, and the AI (grounded in the channel's docs) is a first-class participant in every human thread via @ai / @assist.

The base-spec non-goal line should be annotated (not deleted): *"(Phase 2: channel messaging adds real-time human threads -- see 02_MESSAGING_SPEC.md.)"*

---

## 1. Product vision for the collaboration layer

A channel today = a curated set of the owner's documents + an AI that answers questions grounded in them, for approved members. This spec turns a channel into a **community around that knowledge**:

- **TA <-> student (DM):** a student asks the TA directly when the AI isn't enough; the TA answers once, and can @ai to pull the grounded source right into the thread.
- **student <-> student (DM):** peers help each other, still one tap away from the grounded AI.
- **group room (per channel):** a shared space (like a class Slack channel) where everyone sees announcements, questions, and answers -- and @ai gives the whole room a cited answer from the course material.

**Why this is still TAssist and not "just Slack":** in every thread, the AI is grounded in *the channel's documents* -- the same retrieval, the same display_label citations, the same no-hallucination guarantee. The chat is a delivery surface for grounded knowledge, not a generic messenger.

---

## 2. Locked product decisions (from review)

1. **DM scope -- full Slack:** any approved participant <-> any approved participant (member<->member, member<->owner). Owner does **not** restrict who DMs whom.
2. **Owner powers:** participate + moderate the **group room** (delete any group message), and **enable/disable** the group room per channel. Owner is **not** an observer of member<->member DMs.
3. **Re-join:** reuse the existing DM/group history; nothing archived on leave.
4. **Read state:** read receipts + unread badges in v1.
5. **Group identities:** real display names shown (Slack-style). Distinct from section 7.5 *file* privacy (filenames stay hidden; AI citations still use display_label).
6. **AI trigger:** @ai **and** @assist both invoke a grounded AI turn in any thread.
7. **AI is a first-class participant** in DMs and the group room (not just private AI chat).

---

## 3. The complete surface: chat types in TAssist after this spec

| Type | Participants | AI? | Exists today? |
|---|---|---|---|
| **Private AI chat** (REGULAR/FOLDER) | 1 user + AI | grounded in user's own library | YES v1 -- untouched |
| **Channel AI chat** | 1 member + AI | grounded in channel docs | YES v1 -- untouched |
| **Channel DM** (NEW) | any 2 approved participants + AI on demand | grounded in channel docs via @ai | NO -- this spec |
| **Channel group room** (NEW) | all approved + owner + AI on demand | grounded in channel docs via @ai | NO -- this spec |

The two existing types are the RAG product. The two new types are the collaboration layer. They share the RAG engine but live in separate tables (section 5) so the AI chat semantics never change.

---

## 4. Invariants -- MUST NOT break

1. **AI chat untouched.** ChatScope{REGULAR,FOLDER,CHANNEL} + SSE RAG flow work exactly as today. New messaging uses **new tables**, never repurposes chat/message.
2. **RAG invariant.** Raw files never reach the LLM. @ai in any thread uses the existing channel retrieval (scoped to channel_file), citations by display_label, never real filenames (section 7.5). No hallucination -- grounded-fallback applies.
3. **Membership gates everything.** Only APPROVED participants (or the owner) read/post. PENDING/REJECTED/BANNED/LEFT are denied, checked **live** (never cached). If one DM party loses access, the thread **freezes** (history readable by the still-allowed side, no new messages).
4. **Owner authority is scoped.** Owner moderates the **group room** (delete any message) and toggles it on/off. Owner is a *normal* party in DMs and **cannot read DMs they're not in**.
5. **File privacy holds (section 7.5).** Filenames never leak; AI citations use display labels only. (Group *identities* are visible -- that's people, not files -- a deliberate, separate decision.)
6. **No silent failure.** Sends are transactional; failures surface clean error envelopes ({error:{code,message,...}}), never swallowed 500s.
7. **Additive migrations only.** New tables + one safe ALTER TABLE channel ADD COLUMN ... DEFAULT. No destructive changes. Existing 208 tests stay green.
8. **Quota/rate limits respected.** @ai turns count against the invoker's existing AI quota (section 16). Human sends get a basic per-user rate limit (Bucket4j, reusing existing infra).

---

## 5. Data model (additive)

### 5.1 enums
```
conversation_kind   = ('DM','GROUP')
msg_sender_kind     = ('HUMAN','AI')
```

### 5.2 conversation
```
id             UUID PK
channel_id     UUID NOT NULL REFERENCES channel(id) ON DELETE CASCADE
kind           conversation_kind NOT NULL
participant_a  UUID REFERENCES app_user(id) ON DELETE CASCADE   -- DM only; canonical a<b. NULL for GROUP
participant_b  UUID REFERENCES app_user(id) ON DELETE CASCADE   -- DM only. NULL for GROUP
created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()   -- bumped per message; inbox sort
CHECK ((kind='DM' AND participant_a IS NOT NULL AND participant_b IS NOT NULL AND participant_a<>participant_b)
    OR (kind='GROUP' AND participant_a IS NULL AND participant_b IS NULL))
```
- DM unique: UNIQUE(channel_id, participant_a, participant_b) WHERE kind='DM' -- callers sort the two UUIDs so a pair maps to one row (idempotent open; re-join reuse).
- GROUP unique: UNIQUE(channel_id) WHERE kind='GROUP'.

### 5.3 conversation_message
```
id               UUID PK
conversation_id  UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE
sender_kind      msg_sender_kind NOT NULL
sender_id        UUID REFERENCES app_user(id) ON DELETE SET NULL   -- human author; NULL for AI/deleted-account
content          TEXT NOT NULL
citations        JSONB NOT NULL DEFAULT '[]'   -- AI messages only; display_label based
created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
deleted_at       TIMESTAMPTZ                    -- soft delete (tombstone)
CHECK ((sender_kind='HUMAN' AND sender_id IS NOT NULL) OR sender_kind='AI')
```
Index: (conversation_id, created_at ASC).

### 5.4 conversation_read
```
conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE
user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE
last_read_at    TIMESTAMPTZ NOT NULL DEFAULT now()
PRIMARY KEY (conversation_id, user_id)
```
Unread(user,conv) = count(messages where created_at>last_read_at AND sender_id<>user AND deleted_at IS NULL).

### 5.5 channel alter
```
ALTER TABLE channel ADD COLUMN group_chat_enabled BOOLEAN NOT NULL DEFAULT TRUE;
```

---

## 6. Access control (authorization matrix)

canParticipate(u,C) = u==C.owner_id OR u has APPROVED membership in C.

| Action | Allowed |
|---|---|
| Open DM with target T | canParticipate(me,C) AND canParticipate(T,C) AND me!=T |
| Read DM | me in {participant_a, participant_b} |
| Post DM | both participants still canParticipate |
| List my DMs | any participant (returns DMs where me is a party) |
| Read/post group | canParticipate(me,C) AND C.group_chat_enabled |
| Delete own message | sender_id==me (any thread) |
| Delete any group message | me==owner (group only) |
| Toggle group on/off | me==owner |
| @ai | any who can post in that thread |

Revocation is live. Owner != membership row (special-cased, mirrors section 14.10). Owner cannot read member<->member DMs.

---

## 7. AI-in-thread (the TAssist heart)

Trigger: message contains standalone @ai or @assist (case-insensitive).
1. Save + deliver the human message first.
2. Run the **existing channel RAG path** (retrieval scoped to channel_file, ChatStreamService/generation) with the human text (token stripped) as the query.
3. Save AI reply as conversation_message{sender_kind:'AI', citations:[display_label...]}, deliver to all thread participants.
4. No hits -> grounded-fallback ("not from the channel's documents"), never hallucination.
5. @filename inside the message resolves against **channel files** (not private library), exactly like channel AI chat.
6. Quota: AI turn counts against the invoking user (existing QuotaService). Rate-limited like normal AI usage.
7. AI streams its answer (reuse SSE token streaming); other participants see it stream in too.

---

## 8. Realtime delivery

- **Transport:** reuse SSE (no new WebSocket dependency -- honors "nothing new breaks the stack").
- GET /conversations/{id}/stream -> server pushes message, deleted, read, and AI token/done events for that conversation.
- **Fan-out:** ConversationEventBus bean holding SseEmitters keyed by conversationId. Interface-based so it can swap to **Redis pub/sub** (Redis already a dependency) when multi-instance. v1 = single-instance in-process (documented limitation).
- **Fallback:** dropped SSE -> client re-fetches /messages (reuse the hardened "don't treat post-completion close as error" client we already built).
- Emitters pruned on completion/timeout.

---

## 9. API surface

```
# participants + DMs
GET    /api/channels/{c}/participants                 -> people I can DM (approved+owner, minus me)
POST   /api/channels/{c}/dm            {targetUserId} -> open-or-create DM (idempotent)
GET    /api/channels/{c}/dms                          -> my DM inbox (+unread, last preview, updatedAt)

# group
GET    /api/channels/{c}/group                        -> group ConversationView (403 if disabled/!participant)
PUT    /api/channels/{c}/group/enabled {enabled}      -> owner toggles group_chat_enabled

# shared conversation ops (DM or GROUP)
GET    /api/channels/{c}/conversations/{id}/messages?before&limit=50
POST   /api/channels/{c}/conversations/{id}/messages  {content}     -> human msg (+AI turn if @ai/@assist)
POST   /api/channels/{c}/conversations/{id}/read      {upTo?}       -> mark read
DELETE /api/channels/{c}/conversations/{id}/messages/{mid}          -> soft delete
GET    /api/channels/{c}/conversations/{id}/stream                  -> SSE

# global (cross-channel) unread summary for nav badges
GET    /api/me/unread                                 -> [{channelId, dmUnread, groupUnread}]
```

### DTOs
```
ConversationView { id, channelId, kind, otherParticipant?{userId,displayName}, isGroup, lastMessagePreview?, unreadCount, updatedAt }
MessageView      { id, senderKind, sender?{userId,displayName}, content, citations, createdAt, deleted, seenBy? }
ParticipantView  { userId, displayName, isOwner }
```

---

## 10. UI / UX

New routes (extend section 14):
- /c/@user/messages -- the channel's messaging home: DM inbox + pinned "# Group" (if enabled) + "New message" picker.
- /c/@user/messages/:conversationId -- a thread (DM or group). Composer with @-mention autocomplete (reuse the picker we built) that also suggests @ai, @assist, and channel files + participants.
- Owner channel settings gains a **"Group chat"** toggle + moderation affordance (delete any group message).
- **Nav badges:** unread counts on the channel entry in the sidebar (reuse the "Joined" section) + per-thread in the inbox.
- **Message rendering:** human messages (name + avatar-initial + bubble), AI messages (distinct styling + citation chips -> snippet drawer, reusing existing chat components), tombstones ("message deleted"), "Seen"/"read by N".
- Dark-mode + styled-dialog compliant (reuse useDialog for delete confirms).

---

## 11. Edge cases (all must be handled)

1. **Party banned/kicked/leaves mid-DM** -> thread freezes; next post 403; SSE closes; other side still reads history.
2. **Channel deleted** -> CASCADE removes all conversations/messages/reads. No orphans.
3. **Re-join** -> same DM + group history reused (keyed by pair / channel).
4. **Double-open / two devices** -> idempotent POST /dm; unique index prevents dup threads.
5. **Deleted account** -> sender_id SET NULL; renders "former member".
6. **@ai retrieval error / no hits** -> grounded-fallback inline; human messages unaffected.
7. **Long message** -> cap (default 4000) -> clean 422.
8. **SSE reconnect storm** -> emitters pruned; client backs off; re-fetch on reconnect.
9. **Self-DM** -> blocked (target!=self).
10. **Non-participant opens conversation** -> 403.
11. **Group disabled while someone's in it** -> their next post 403; read of history still ok; UI hides the room.
12. **Owner deletes a member's group message** -> tombstone; original sender notified via stream event.
13. **Concurrent delete+read** -> soft delete idempotent; reading a just-deleted msg shows tombstone.
14. **Spam** -> per-user human send rate limit (Bucket4j, default 30/min); AI turns use AI quota.
15. **@ai + @filename together** -> file resolves against channel files; AI grounds on it.
16. **Owner never injected into existing member<->member DMs** -> owner is not auto-added to DMs they aren't part of.
17. **Unread accuracy across devices** -> last_read_at is max-wins; opening on any device advances it.
18. **Message ordering under clock skew** -> server created_at is authoritative; client sorts by it.
19. **AI turn while sender navigates away** -> AI reply still saved + delivered to remaining participants (background, like auto-title).
20. **Empty group room** -> clean empty state; still listed if enabled.

---

## 12. Build order (Phase 1 = DM, Phase 2 = GROUP)

Same stop-and-verify, granular-commit discipline. Each step: build -> test -> verify -> commit -> push.

**Phase 1 -- DM**
- M1 V9__conversations.sql (tables, enums, indexes, channel alter). Verify migrate clean + data intact.
- M2 Domain models + enums + repo ports + unit tests.
- M3 Persistence (JPA entities/adapters/mappers) + integration tests.
- M4 ConversationService (open-or-create, list, post, delete, read) with full section 6 access + section 11 edge tests.
- M5 @ai/@assist -> existing retrieval/generation; grounded + fallback tests.
- M6 REST controllers + DTOs + clean error envelopes.
- M7 ConversationEventBus + SSE stream + emitter lifecycle.
- M8 Frontend: messages home, DM thread, composer (reuse mention picker), optimistic send, read receipts, unread badges, soft-delete UI (reuse useDialog).
- M9 Full-suite gate (208 green) + two-sided manual test (owner + member incognito + second member).

**Phase 2 -- GROUP** (reuses M1-M9 shapes)
- G1 group open/list + group_chat_enabled toggle endpoint + owner settings UI.
- G2 group posting/read/delete (owner moderation).
- G3 group SSE fan-out (multi-subscriber).
- G4 nav unread badges (/me/unread) + polish.

---

## 13. Nits to finalize during build (low-stakes)
- Human send rate limit (default 30/min/user).
- Max message length (default 4000).
- Group "read by" = count in v1; per-person "seen by" deferred.
- @ai streaming vs one-shot in group (default: stream, same as AI chat).

---

## 14. What we are explicitly NOT doing in this spec (keeps scope honest)
- No reactions, edits, threaded replies, typing indicators (Phase 3 candidates).
- No file attachments in messages (files stay library/channel docs; referenced via @mention for AI).
- No cross-channel DMs (a DM is always within one channel's membership).
- No voice/video. No message search in v1 (Phase 3).
- No push notifications (in-app unread badges only in v1).
