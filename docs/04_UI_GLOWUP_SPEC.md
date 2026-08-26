# 04 — UI Glow-Up Spec: "Study Space, Not a Chat App"

**Status:** DRAFT v1 for review · **Extends:** `01_TASSIST_SPEC.md` §15 (design tokens), `03_CHANNEL_SHELL_SPEC.md` · **Type:** Frontend visual/interaction polish

> **Rule of this pass:** compose with the EXISTING tokens in `design/theme.css` (the file says "Do not invent new ones" — we honour that). We add *structure, rhythm, motion, and interaction patterns*, not new colors. Zero backend/API/behaviour change. Every messaging + RAG guarantee stays exactly as built.

---

## 0. The thesis

TAssist is a **teaching-assistant study space**, not a corporate chat tool. Our tokens already say this: warm **paper** surfaces (`#FDFAF4`, not white), a **scholarly deep-indigo** brand (`#3E2A93`), soft human accents (peach/mint/amber/rose), and a dark mode that's "paper at night" (warm charcoal, never cold gray).

So the glow-up direction is:

**"Slack's proven interaction patterns, rendered as warm paper."**

We take what makes Slack *feel* fluid — author-grouped message rows, hover-reveal actions, a living sidebar, tight vertical rhythm, quiet micro-motion — and render it in TAssist's paper-and-indigo identity instead of Slack's cold aubergine/white. This satisfies all three goals at once: faithful Slack UX + our own identity + a stunning bar.

**The signature element** (where we spend our boldness): the **AI turn**. When someone `@ai`s in a thread, the AI's answer is not a chat bubble — it renders as a **"margin note" / study annotation**: an indigo left-rule, a soft paper-elevated card, the answer typeset like a passage from a book, with citation chips styled as **footnote markers** that open the source in the margin drawer. This is the one thing no other chat app has, and it's true to "grounded in your documents." Everything else stays quiet and disciplined around it.

---

## 1. Non-negotiables

1. Compose with existing tokens only (surfaces, text, brand, accents, type scale, radii, shadows). No new hex values unless we consciously extend the token file with your sign-off.
2. Zero backend/API/DB/behaviour change. Pure presentation + interaction.
3. Quality floor (per design skill): responsive to mobile, visible keyboard focus, `prefers-reduced-motion` respected, dark mode correct for every new surface.
4. Both test suites stay green; granular commits per component; spec-first.
5. Restraint: boldness goes into the AI margin-note signature. Everything else is quiet precision.

---

## 2. Design system deltas (built on §15 tokens)

We're not changing tokens; we're adding a small set of *conventions* on top, expressed as utility patterns:

**Type roles** (using existing `--font-*` scale):
- **Display** (channel headers, empty-state titles): `--font-xl`/`--font-2xl`, weight 600, tight tracking.
- **Sender name**: `--font-sm`, weight 600.
- **Body**: `--font-md`, line-height 1.5.
- **Meta** (timestamps, labels): `--font-2xs`/`--font-xs`, `text-faint`, often uppercase-tracked for section headers.

**Spacing rhythm:** message rows use a consistent 8px inner / 2px between-grouped / 16px between-authors rhythm. Rail rows 32px tall, 2px gap. This tight, consistent rhythm is 80% of the "Slack feel."

**Elevation:** flat surfaces by default; `--shadow-1` for the composer and hover toolbars; `--shadow-2` only for modals/drawers. No shadow soup.

**Motion:** 150ms ease for hover/color; a 180ms fade-slide-up for newly-arriving messages; composer grows smoothly. All gated behind `prefers-reduced-motion: reduce` → none.

---

## 3. Component-by-component plan

### 3.1 Message list — the biggest change (Slack-style rows, not bubbles)
**Now:** left/right chat bubbles (WhatsApp style). This is the #1 reason it doesn't read as Slack.
**Target:**
- **Left-aligned rows** for everyone (no right-alignment for "me" — Slack doesn't do this).
- **Avatar** (28px, rounded) in a left gutter; **sender name** (semibold) + **timestamp** (faint, inline) on the first line; body below.
- **Author grouping:** consecutive messages from the same sender within ~5 min collapse — avatar+name+time shown once, following lines just show the body indented into the gutter (timestamp appears on hover at the left edge, Slack-style).
- **Hover toolbar:** a small floating action bar (top-right of the row) revealed on hover — holds Delete (and future: react, reply). Quiet until hover.
- **"me" differentiation** is subtle: a faint paper-sunken row background on hover for all; no color-coded sides.
- **Day dividers:** a centered hairline with a date pill ("Today", "Yesterday", "Mar 3") between day boundaries.

### 3.2 The AI turn — the SIGNATURE
**Now:** a slightly-bordered indigo bubble with pill citation chips.
**Target — "margin note":**
- Full-width study-card, **indigo left-rule** (3px `--primary`), paper-elevated (`bg-elev`, `--shadow-1`), generous padding.
- A small **"AI · grounded in N sources"** eyebrow in indigo with the `Sparkles` glyph.
- Answer typeset at `--font-md`, line-height 1.6, like a book passage.
- Citations render as **superscript footnote markers** ¹ ² ³ inline-adjacent + a "Sources" footer row of chips; clicking either opens the existing `SnippetDrawer` (the "margin").
- Grounded-fallback ("I couldn't find that in the documents") gets a distinct quiet amber treatment, not an error.

### 3.3 Channel rail — make it *live*
**Now:** functional list, flat.
**Target:**
- Section headers (`# GROUP` isn't a header — DIRECT MESSAGES / CHANNEL are) in `--font-2xs` uppercase, `text-faint`, with tracking.
- **Unread rows: bold `text` weight 600**; read rows: `text-muted`. This contrast is how Slack signals unread at a glance.
- **Active row:** solid `bg-sunken` + a 3px indigo left-bar + `text` color.
- Avatars/initials on DM rows; a `Users`-glyph tile on # Group.
- Hover: `bg-sunken` at 60%, 150ms.
- Unread count badges right-aligned, indigo, only when >0.
- "New message" as a quiet `+` in the DM section header (already there) — refine hover.

### 3.4 Composer — a real input, not a textarea
**Target:**
- Paper-elevated rounded container, `--shadow-1`, focus-within indigo ring.
- Left: a subtle `@`/attach affordance row (visual for now; @ works via typing).
- Auto-grow (already have), max height, then scroll.
- Send button: indigo, disabled state at 40%, subtle scale on press.
- Tiny hint: "**Enter** to send · **Shift+Enter** for a new line" in `--font-2xs text-faint`, right-aligned under the input.
- The @-suggestion dropdown gets the paper-card + `--shadow-2` treatment with keyboard-highlight rows (already functional; restyle).

### 3.5 Thread header
**Target:** avatar/glyph + title (semibold) + subtitle (faint), a hairline bottom border, and (future) a right-side action slot. Group shows "# Group · N members" once we have counts.

### 3.6 Empty states — warmth, not "No messages"
- Group: "This is the start of **# Group** — everyone in @handle is here."
- DM: "This is the beginning of your conversation with **{name}**."
- No DMs in rail: "No conversations yet — start one with +."
- Files: "No documents yet — the AI answers from what's added here."
All in the interface's voice, sentence case, an invitation to act.

### 3.7 Micro-motion
- New message: 180ms fade + 6px slide-up.
- Rail active/hover: 150ms.
- Composer grow: height transition.
- Drawer/modal: existing.
- All behind `prefers-reduced-motion`.

---

## 4. Build order (compile-green + one commit each)

- **U0 — Motion + type utilities.** Add reduced-motion-safe keyframes/utilities to a small `design/motion.css` (or Tailwind layer). No component change yet.
- **U1 — Message row rebuild.** `MessageRow` (author-grouped, avatar gutter, hover toolbar, day dividers). Replaces bubble layout in `MessageBubble`. Pure-logic bits (grouping predicate, day-divider computation) → `logic.ts` + Vitest.
- **U2 — AI margin-note signature.** The study-card AI turn + footnote citations.
- **U3 — Channel rail glow-up.** Unread weight, active bar, hover, section headers, avatars.
- **U4 — Composer.** Paper container, focus ring, hint, restyled suggestion dropdown.
- **U5 — Thread header + empty states + day dividers polish.**
- **U6 — Motion pass.** Wire the fade-slide-up + transitions, verify reduced-motion.
- **U7 — Verify + tests + screenshots.** Full gauntlet; add Vitest for new pure logic; eyeball light+dark, mobile width.
- **U8 — Commits + BUILD_LOG + push + CI green.**

Each U-step is independently compile-green and committed `U<n>: <what>`.

---

## 5. Pure logic to unit-test (keep the "both sides tested" bar)
- `groupsWith(prev, cur)` — should this message collapse under the previous (same sender, <5min, both human)?
- `dayDividerLabel(iso, now)` — "Today" / "Yesterday" / "Mar 3".
- `initials(name)` — avatar fallback.
These are framework-free in `messaging/logic.ts` (or a new `ui/logic.ts`), covered by Vitest.

---

## 6. Open decisions for sign-off
- **D-UI1:** AI turn as "margin note" study-card (the signature) — agree this is where we spend boldness? *Rec: yes.*
- **D-UI2:** Drop left/right bubbles entirely for Slack-style left-aligned rows? *Rec: yes — it's the core of the feel.*
- **D-UI3:** Avatars — we have display names; use colored initials tiles (deterministic color from name) since there are no uploaded avatars? *Rec: yes, initials tiles.*
- **D-UI4:** Scope — do all of U1–U6 this pass, or ship U1–U3 (rows, AI, rail — the high-impact core) first and do composer/motion in a second pass? *Rec: your call on appetite.*

---

*End of draft. No glow-up code past U0 until this is signed off and D-UI1…D-UI4 are decided.*
