# 05 — TAssist Glow-Up: Research-Grounded Design Bible

**Status:** DRAFT v1 for review · **Supersedes:** `04_UI_GLOWUP_SPEC.md` (folds it in) · **Type:** Full visual + interaction + landing redesign · **Constraint:** frontend only, zero backend/API/behaviour change

> This is the single source of truth for the TAssist glow-up. It is grounded in real research (Slack's own design team writing, the 2026 AI-citation UX canon, cinematic SaaS-landing analysis, education/calm-focus UX, and the WCAG motion/focus floor). Every decision cites *why*. We build only after this is signed off.

---

## PART A — RESEARCH DIGEST (what we learned, and the receipts)

### A1. Slack's own design principles (from slack.design + brand guidelines)
- **Canonical surface is LIGHT**: white/bright content with a deep **aubergine `#4A154B`** sidebar — the *inverse* of dark-first tools. The dark sidebar is a "calm, always-present navigation anchor"; content is "bright and open."
- **Aubergine was a deliberate rejection of corporate blue** — "friendly where enterprise software is formal." Slack differentiated by refusing the category default. *(This is the single most important strategic lesson for us — see B1.)*
- **The 4 logo colors (blue/green/yellow/red) do NOT scatter through the UI** — they live in the hashtag mark and marketing. In-product, aubergine is the *sole* brand anchor. Restraint.
- **"Desk metaphor"**: Slack is personal, like your desk. Theming is core DNA. They simplified 9 theme inputs → 4, mapped to 20 predetermined palettes with auto-contrast.
- **Never sacrifice productivity/density for prettiness.** The redesign kept information density fully intact and added polish *around* it: gradients, transparent surfaces, rounded buttons/avatars, softer borders, elevation/depth for hierarchy.
- **Bloops** — real-time activity signals over top-level nav (a sender's avatar temporarily replaces the DM icon on a new message) to cut "pogo-sticking."
- **Peeks** — hover a nav tab to preview its contents without navigating away.

### A2. The 2026 AI-citation UX canon (Perplexity / Claude / ChatGPT / Notion / Glean)
- **"Citations are the trust currency of every AI answer."** If a user can't trace a claim to a source in ~2 seconds, the answer is worth nothing — and one unsourced-looking claim poisons trust in the whole product.
- Ranked patterns by (Visibility + Trust) / Effort. Top matches for TAssist (internal-docs Q&A):
  1. **Inline numbered citations + hover preview** — score 5.0, table stakes. Superscript ¹²³ at the claim; hover/tap → popover with source label + excerpt (cap ~200 chars) + "open source". Real focusable `<a>`/`<button>`, Escape to dismiss, tap-to-open on mobile.
  2. **Missing-source disclosure** — score 4.5. Never style an unsourced/fallback answer like a sourced fact. This is literally TAssist's promise (grounded-only, no hallucination).
  3. **Deep-link to source passage** — score 3.0, *"best for internal knowledge base and document Q&A"* (that's us). Land on the exact passage, highlighted — not the doc top. We already have `SnippetDrawer`; evolve it toward this.
  4. **Source-card sidebar/strip** — for multi-source answers (3+), numbered consistently with inline chips.
- Also: **stream token-by-token** (loads-all-at-once after 8s "feels broken"); **empty prompt paralysis** — never a bare "Ask anything…", give example prompts; **ARIA live regions** for streaming answers.

### A3. Cinematic SaaS landing (2026 canon: Linear, Notion, Attio, Cursor)
- **The bar:** "cinematic motion, editorial typography, ruthless restraint — feel like brand films, not product pages."
- **Hero = live product doing real work in the first 5s** (Linear's agent, Attio's "Ask Attio"). Replaces the static screenshot. **Our hero: a live/animated ask → AI margin-note answer with citations.**
- **Bento grid 2.0** for features: rounded tiles, hover micro-interactions, size-as-hierarchy.
- **Scroll-triggered reveals**; story-driven hero (problem→solution, who it's for); persistent-but-earned CTA.
- **The default trap (AVOID):** "dark-as-default + one neon accent" (Linear-purple, Cursor-cyan) — ~75% of design-led SaaS look identical. Notion is the counter-proof: warm, light-primary, restraint — "warm design serves both prosumers and enterprise."

### A4. Education / calm-focus UX (our actual domain)
- **Reduce cognitive load** — students already carry course stress; the tool must feel *calm*. Simplicity, clean hierarchy, familiar patterns.
- **Confidence & control** — timely feedback + progress visibility keep students engaged.
- **AI trust gap is real** — 86% of students use AI, but adoption ≠ trust. Verifiable (cited) answers are what make AI usable for academic work. Our citation-first design *is* the trust mechanism.
- **Warmth, not childishness** — for uni students + TAs, Notion-style warmth wins; cartoon mascots/bitmoji would undercut academic credibility. **Decision: friendly copy + micro-delight, NO mascot.**

### A5. Motion & accessibility floor (WCAG 2.x, MDN)
- `prefers-reduced-motion` on **every** animation; reduced → remove or swap to static. Content must never depend on motion (a slide-in alert must be visible without the slide).
- Focus indicators: visible, **3:1 contrast, ≥2px** (2.4.13). Tap targets ≥24×24 (AA).
- No autoplay >5s without pause; no large parallax (vestibular safety). Purposeful, short.
- Test with all motion off — the message must still land.

---

## PART B — IDENTITY FOUNDATION

### B1. Strategic thesis (the one big idea)
Slack won by **rejecting the category default** (corporate blue → friendly aubergine). The 2026 AI-SaaS default is **dark + one neon accent**. So our differentiation move is the same as Slack's: **reject the default.** TAssist is **warm paper, light-first, scholarly** — a *study space*, not a chat app, not a dark neon dashboard.

Our existing tokens already encode this (warm cream `#FDFAF4`, deep indigo `#3E2A93`, "paper at night" dark mode). Research **confirms** this is a differentiated, defensible identity — so **D-05-1: we keep and refine the warm-paper + indigo palette; we do NOT chase the dark-neon trend.** (This resolves the "open to bolder palette" question: research says our lane is *already* the bold, non-default choice — we sharpen it rather than abandon it.)

### B2. Positioning line (working)
**"A calm place to study, where every answer shows its work."**
- "calm place" → paper, light, low cognitive load (A4).
- "study" → desk metaphor, our domain (A1, A4).
- "shows its work" → citations as trust currency (A2), our RAG promise.

### B3. The signature (where we spend all our boldness)
**The AI answer as a "margin note" / study annotation** — not a chat bubble. Indigo left-rule, paper card, book-passage typesetting, **inline superscript footnote citations** that hover-preview and open the source passage in the margin drawer, and a distinct honest treatment for the grounded-fallback ("not in your documents"). This single element unifies A1 (elevation/depth), A2 (the entire citation canon), A4 (trust), and B2 (shows its work). Everything else stays quiet so this sings (Chanel's rule).

---

*(Parts C–F below: palette refinement, typography, per-page/per-component redesign, landing page, motion system, build order. Continued in next section.)*

---

## PART C — VISUAL SYSTEM REFINEMENT

### C1. Palette (refine, don't replace — D-05-1)
Keep all existing `--tassist-*` tokens. Research-driven *additions* (to be added to `theme.css` with sign-off, since the file says "don't invent" — we extend deliberately, not randomly):

- **`--primary-wash`** (indigo @ ~6% alpha) — for AI margin-note card fills / hover tints. Slack uses transparent surfaces for lightness (A1).
- **`--primary-rule`** (solid indigo) — the 3px left-rule on AI notes + active rail rows.
- **`--focus-ring`** = indigo @ 3:1+ contrast, 2px — WCAG 2.4.13 (A5). One token, used everywhere focus appears.
- **A deterministic avatar palette** — 6 tints derived from existing accents (peach/mint/amber/rose + two indigo shades) for initials tiles (no uploaded avatars). Assigned by hashing displayName.
- **Semantic "grounded" vs "ungrounded"**: grounded AI = indigo family; ungrounded/fallback = amber family (warning, not error — A2 missing-source disclosure). Never rose/red — a fallback is honest, not a failure.

Logo-color energy (like Slack's 4 hues) is reserved for the **landing page + empty-state illustrations**, never scattered in the app chrome (A1 restraint).

### C2. Typography
Current: fluid `--font-*` scale exists; faces not yet distinctive. Per the design skill, type carries personality.
- **Display face** (headers, landing hero, empty-state titles): a characterful face used with restraint. Candidate direction: a warm humanist serif or a confident geometric sans with real character — NOT the AI-default "Instrument Serif italic." **D-05-2: pick the display face during C-build after trying 2–3 in-situ; document the choice.**
- **Body/UI face**: a clean, highly-legible sans (Inter-class) for density and screen legibility (A1 density).
- **Mono/utility**: for citations' footnote numerals, code, timestamps — a mono or tabular-figure setting so numbers align.
- **Type roles** (using `--font-*`): Display 2xl/xl @600 tight; Sender-name sm @600; Body md @1.5lh; AI-note body md @1.6lh (book passage); Meta 2xs/xs faint, section headers uppercase-tracked.

### C3. Spacing, elevation, radius (Slack-grade density + softness — A1)
- **Rhythm:** message rows 8px inner / 2px grouped / 16px author-break; rail rows 32px, 2px gap. Tight and consistent = the "Slack feel."
- **Elevation:** flat by default; `--shadow-1` for composer + hover toolbars; `--shadow-2` only modals/drawers. No shadow soup.
- **Radius:** lean on existing `--r-*`; rounded avatars + buttons (A1 "softened tone").

---

## PART D — PAGE-BY-PAGE & COMPONENT-BY-COMPONENT REDESIGN

Every surface in the app (17 pages + shell parts), what it is, the problem today, and the target. Grouped by area.

### D0. Global shell
- **`LeftRail`** — global nav (home, library, channels, discover, theme, settings, logout). *Target:* section headers uppercase-tracked-faint; active item indigo left-bar + sunken bg; **Bloops-lite** (A1) — a subtle unread dot on the "Joined" channel rows fed by future `/api/me/unread`; hover states 150ms; rounded avatars for channels.
- **`AppLayout`** — global rail + content. *Target:* unchanged structure; ensure consistent page-padding tokens.

### D1. Auth (`LoginPage`, `SignupPage`, `OAuthCompletePage`)
- *Problem:* functional forms, no brand moment. *Target:* split layout — left: the form (calm, generous, one clear CTA, inline validation with a checkmark on valid, A4 feedback); right: a warm "paper" brand panel with the positioning line (B2) + a static preview of the margin-note signature. Reduced-motion-safe. Focus rings everywhere (A5).

### D2. Dashboard + Library (`Dashboard`, `FolderPage`, `NewChatPage`, `ChatPage`)
- **Dashboard** — the private library home. *Target:* a warm welcome header ("Good evening, {name}"), library folders as soft bento tiles (A3 bento) with hover lift, recent chats list with clear hierarchy. Empty state: an invitation to upload the first doc (A2/A4 "empty screen is an invitation to act").
- **`FolderPage`** — files in a folder. *Target:* clean file list/grid, file-type glyphs, calm density.
- **`NewChatPage` / `ChatPage`** — private AI chat over the library. *Target:* adopt the same **message-row + AI-margin-note** system as messaging (D4/D5) so private and channel AI feel unified. Example-prompt chips on empty (A2 anti-paralysis). Token streaming already present — style the stream cursor.

### D3. Channels — discovery & management
- **`DiscoverChannelsPage`** — browse public channels. *Target:* bento cards, each channel a soft tile with name/@handle/description/member-count + Request/Enter CTA; hover lift; search field with focus ring.
- **`MyChannelsPage`** — owner's channel list. *Target:* same tile language; each tile enters the shell (already wired). Empty: "Create your first channel."
- **`CreateChannelPage`** — creation form. *Target:* calm multi-field form, inline validation, live @handle preview, clear primary CTA.
- **`ChannelManagePage`** (in-shell, owner) — Overview/Files/Members tabs. *Target:* refined tab bar (active indigo underline), Members table with status pills + row actions (approve/kick/ban) with `useDialog` confirms, Files tab with display-label management.

### D4. Channel shell (`ChannelShell`, `ChannelRail`, `ChannelIndex`, `ChannelAboutPage`, `ChannelFilesPage`)
- **`ChannelRail`** — THE living sidebar (A1). *Target:* section headers (DIRECT MESSAGES / CHANNEL) uppercase-tracked; **unread rows bold `text`, read rows `text-muted`** (A1 the unread-weight trick); active row = sunken bg + 3px indigo left-bar; initials-tile avatars (C1); unread count badges indigo, right-aligned, only >0; # Group with a Users glyph; "New message" + in the DM header; hover 60% sunken 150ms.
- **`ChannelAboutPage`** (non-participant) — request access. *Target:* a proper channel "cover" — big name, description, expectations card, member-count, and the request-access flow with pending/rejected/banned states in honest tones (amber pending, quiet rose banned). This is a mini landing for the channel.
- **`ChannelFilesPage`** (member read-only) — what the AI answers from. *Target:* calm file list, display-label only (§7.5), a one-line "the AI answers from these" explainer, warm empty state.
- **`ChannelIndex`** — redirect only; no UI beyond a branded loading shimmer.

### D5. Messaging (`ThreadPage` + message components) — the heart
- **Message list — Slack-style rows (replaces bubbles).** Left-aligned rows, avatar gutter, sender name @600 + faint inline timestamp, body below. **Author grouping**: consecutive same-sender <5min collapse (avatar+name once; following lines show body, timestamp on hover at left edge — A1 density). **Hover toolbar** top-right (delete now; react/reply later — quiet until hover). **Day dividers** (centered hairline + date pill: Today/Yesterday/Mar 3).
- **The AI turn — THE SIGNATURE (B3).** Full-width study-card, 3px indigo left-rule, `--primary-wash` fill, `--shadow-1`; eyebrow "AI · grounded in N sources" with Sparkles; answer typeset md/1.6 like a passage; **inline superscript ¹²³** citations (A2 #1) with hover-preview popover (label + ≤200-char excerpt + open); a "Sources" footer strip of numbered chips (A2 #4) matching the superscripts; clicking any → `SnippetDrawer` opens the passage (A2 #3 deep-link, highlighted). **Grounded-fallback** = distinct amber-family treatment, honest, never error-styled (A2 #2). Streaming: token-by-token with a soft cursor; ARIA live-polite (A2/A5).
- **Composer.** Paper-elevated, `--shadow-1`, focus-within indigo ring; auto-grow→scroll; @-suggestions dropdown (paper card, `--shadow-2`, keyboard-highlight) over @ai/@assist/participants/files; send button indigo w/ disabled + press-scale; hint "**Enter** to send · **Shift+Enter** for newline" 2xs faint.
- **Thread header.** Avatar/glyph + title @600 + subtitle faint; hairline border; # Group shows "· N members" when counts exist.
- **Empty states** (A4 warmth): Group "This is the start of **# Group** — everyone in @handle is here." DM "The beginning of your conversation with **{name}**." Rail-no-DMs "No conversations yet — start one with +."

### D6. Utility (`SettingsPage`, `NotFoundPage`)
- **`SettingsPage`** — theme toggle (light/dark/system), account, preferences. *Target:* clean settings-list pattern, the theme toggle as a delightful little light/dark paper-flip (reduced-motion-safe).
- **`NotFoundPage`** — *Target:* warm, on-brand, a single "back to your desk" CTA; not a cold 404.

---

## PART E — CINEMATIC LANDING PAGE (`LandingPage`) — IN SCOPE

A brand film, not a product page (A3). Light-first warm-paper (B1), rejecting the dark-neon default.

- **Hero (the thesis, A3 live-demo hero):** headline = positioning line (B2, "A calm place to study, where every answer shows its work"). Beside/below it, a **live, self-playing micro-demo**: a student types a question → the **AI margin-note** answers with animated superscript citations that "write on" → a citation opens the source passage. First 5 seconds show the product's soul doing real work. Reduced-motion → static framed screenshot of the same moment (A5).
- **Scroll-reveal sections** (A3, IntersectionObserver, all reduced-motion-safe):
  1. **The problem** — students ask the same questions at 2am; TAs answer manually. (Problem→solution arc, A3.)
  2. **How it works** — 3 real steps (TA uploads course docs → student asks → grounded answer with citations). Numbered *because it's a genuine sequence* (design-skill: numbering only when order is real).
  3. **Grounded, not guessed** — the citation/trust story (A2). Show a real margin-note with footnotes. This is the differentiator.
  4. **Channels** — Slack-style Q&A over curated docs; the shell shown.
  5. **Bento feature grid** (A3 bento 2.0) — RAG-only privacy, display-label file hiding, realtime, dark mode, etc. — rounded tiles, hover micro-interactions, size-as-hierarchy.
  6. **CTA** — persistent-but-earned (A3): "Start your study space." Sign up.
- **Type:** editorial display face (C2); ruthless restraint (A3). Warm paper surfaces, indigo accents, logo-color energy allowed *here* (C1).
- **Perf/A11y:** load <2s, no autoplay-with-sound, no >5s unstoppable loops, focus-visible throughout, mobile-first responsive (A5).

---

## PART F — MOTION SYSTEM

A small, disciplined motion vocabulary (A3 "restraint", A5 accessibility). Defined once in `design/motion.css`, gated globally:
```
@media (prefers-reduced-motion: reduce) { * { animation: none !important; transition: none !important; } }
```
plus per-component static fallbacks where motion reveals content (A5).
- **150ms** ease — hover/color/bg (rail rows, buttons, chips).
- **180ms** fade + 6px slide-up — a newly-arriving message (content still present without motion).
- **Composer grow** — height transition.
- **Citation superscript** — a gentle "write-on" only on the landing demo, never in-app repeatedly (cognitive load, A4/A5).
- **Drawer/modal** — existing.
- **Theme flip** — a brief paper cross-fade.
- No parallax, no autoplay >5s, no looping decoration (A5 vestibular).

---

## PART G — BUILD ORDER (spec-first, compile-green + one commit each)

Foundation → app core → app breadth → landing → polish. Each step independently green + committed `G-UI<n>:`.

- **G-UI0** — Tokens + motion: extend `theme.css` (C1 additions), add `design/motion.css` (F), pick+wire display/body faces (C2). Screenshot light+dark. No component logic change.
- **G-UI1** — Message-row rebuild (D5 rows, grouping, hover toolbar, day dividers). Pure logic (grouping predicate, day-label, initials) → `logic.ts` + Vitest.
- **G-UI2** — AI margin-note signature (D5): card, superscript citations, hover-preview popover, sources strip, fallback treatment, streaming cursor + ARIA. The centerpiece.
- **G-UI3** — Composer + thread header + empty states (D5).
- **G-UI4** — Channel rail glow-up (D4): unread weight, active bar, avatars, sections, badges.
- **G-UI5** — Channels breadth (D3): Discover/MyChannels/Create/Manage/About/Files tiles + forms.
- **G-UI6** — Dashboard/Library + private chat unification (D2) + auth brand split (D1).
- **G-UI7** — Utility (D6) + LeftRail Bloops-lite (D0).
- **G-UI8** — Cinematic landing page (E).
- **G-UI9** — Motion pass wiring (F) + full reduced-motion/focus/mobile audit.
- **G-UI10** — Verify (gauntlet + Vitest for all new logic) + screenshots light/dark/mobile + BUILD_LOG + push + CI green.

---

## PART H — OPEN DECISIONS FOR SIGN-OFF
- **D-05-1:** Keep+refine warm-paper+indigo (reject dark-neon default)? *Research-backed rec: YES.*
- **D-05-2:** Display typeface — pick during G-UI0 from 2–3 candidates in-situ (avoid Instrument-Serif default). Any face you already love?
- **D-05-3:** Scope/sequence — build all G-UI0→10, or ship the app core (0–4, the daily-driver surfaces) first, then landing (8) + breadth? *Rec: app core first — highest daily impact — then landing.*
- **D-05-4:** Avatars — deterministic initials tiles (no uploads)? *Rec: YES.*
- **D-05-5:** Any Slack "Bloops/Peeks" appetite now (unread dots / hover-preview), or park until `/api/me/unread` exists? *Rec: unread-dot only now, hover-peek later.*

---

*End of draft. No glow-up code past G-UI0 until this is signed off and D-05-1…D-05-5 are decided.*
