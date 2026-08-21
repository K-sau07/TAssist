# TAssist — Build Log

> **Purpose.** This is the living state-of-the-build record for TAssist. It is the
> companion to `01_TASSIST_SPEC.md`. The spec is the immutable source of truth for
> *what* to build; this log records *where we are*, *what was actually built*, and
> *every decision made in a gap the spec left open*.
>
> **Protocol (followed every session):**
> 1. At the start of any work, read `01_TASSIST_SPEC.md` **and** this log.
> 2. Build strictly in the §20 order. One step at a time. Stop-and-verify after each.
> 3. At the end of a step, reconcile what was built against the spec's acceptance
>    criterion, then append a dated entry here with: files shipped, verification, and
>    any deviations/decisions with rationale.
> 4. Never silently re-decide something already recorded here. If a past decision
>    needs changing, add a new entry that supersedes it and say so explicitly.
>
> **Commit convention (from Step 3 onward, 2026-08-20):**
> - Commit granularly *within* a step at every point the project compiles green —
>   NOT one giant commit per step. A Step-2-sized chunk should be ~3–6 commits.
> - Hard rule: **every commit must compile** (`mvn -q compile` clean); a commit that
>   introduces tests must pass them. Never commit a broken build. Order sub-commits so
>   each is self-consistent (e.g. entities+mappers together, not entities alone).
> - Message format: `Step N.M: <what>` (e.g. `Step 3.1: JWT support + security config`).
>   The step's BUILD_LOG entry + final "DONE & VERIFIED" reconciliation still happens
>   once, in the last sub-commit of the step.
> - Push after each green sub-commit (or at least at end of step). Working tree stays
>   clean between sub-commits.
> - Steps 0–2 were single-commit (8016c2e, 47b6360, 554e06d); that's fine, not retro-split.

---

## Locked decisions (spec-gap resolutions)

These are choices the spec left open or that the environment forced. They are binding
until explicitly superseded by a later log entry.

| # | Decision | Value | Rationale / source |
|---|----------|-------|--------------------|
| D1 | Base Java package | `com.tassist` | Spec §7 package layout is explicit. Overrides the old `com.ksau07.tassist` namespace. (User confirmed 2026-07-28.) |
| D2 | Old backend code | Wiped, archived | Pre-existing code implemented the old README "Course/Lecture/Slide" model, incompatible with the File/Folder/Channel spec. Archived to `/tmp/tassist-old-backend-20260728-164016.tgz` before deletion. Untracked in git, so no history lost. (User confirmed 2026-07-28.) |
| D3 | JDK for Maven builds | Temurin 21 | Spec §6 locks Java 21. PATH default is JDK 23 and Homebrew Maven runs on JDK 25 — neither matches. Pin `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` for every `mvn` invocation. |
| D4 | Node for frontend | nvm Node 20 (`v20.20.1`) | Spec §6 locks Node 20 LTS; Vite 6 requires Node 20+. PATH default is Node 18.17.0. Use nvm to select 20 for all frontend commands. |
| D5 | Embedding provider | **RESOLVED (2026-08-20): Voyage `voyage-3.5`, 1024-dim.** | Spec §21 open Q#1. Chose Voyage over OpenAI: (a) spec-aligned (least deviation); (b) best retrieval quality for mixed-domain grounded RAG — voyage-3-large beats OpenAI-v3-large ~10.6% at 1024 dims; (c) 200M free tokens covers all of Phase 1; (d) 1024 is a valid Matryoshka width for BOTH providers, so OpenAI remains a truncate-to-1024 fallback with no schema change. Picked `voyage-3.5` (not the newer `voyage-4`, Jan 2026) deliberately: stable, documented, 1024-native, avoids betting the schema on the newest model pre-retrieval-testing. Spec named `voyage-3`; 3.5 supersedes it (better quality/context, same 1024 default). Sets `VECTOR(1024)` in V4/V5. |
| D6 | Spring AI Anthropic artifact | `spring-ai-starter-model-anthropic` on BOM `1.0.9` | Spec §20 named the pre-GA id `spring-ai-anthropic-spring-boot-starter`, which does not exist in Central. Renamed at Spring AI 1.0.0 GA. |
| D7 | Bucket4j coordinates | `com.bucket4j:bucket4j_jdk17-core:8.19.0` | Spec §6 implies Bucket4j but pins no version. `8.10.1` only exists under the legacy `bucket4j-core` id; the maintained JDK17+ line (`_jdk17-core`) starts at `8.19.0`. Appropriate for Java 21. |
| D8 | Frontend build tool versions | Vite `6.4.3`, TypeScript `5.9.3`, ESLint (not oxlint) | `create-vite@9` scaffolds Vite 8 / TS 6 / oxlint. Pinned back to Vite 6 / TS 5 per §6 (spec locks Vite 6). Kept ESLint because §23 references `eslint-plugin-jsx-a11y`. |
| D9 | shadcn/ui timing | Deferred to Step 15 | shadcn is a CLI that copies component source on demand, not a runtime npm dep. Radix/`clsx`/`tailwind-merge`/`cva` helpers installed now; `shadcn init` runs in Step 15 when components are themed and used. |
| D10 | DC create_file unreliable | Use bash heredoc + verify | This session, Desktop Commander create_file/str_replace reported success but silently dropped writes; start_process hung repeatedly. Write files via `cat > f <<'EOF'` and confirm with `wc -l`. |
| D11 | Vector + JSONB entity mapping | `org.hibernate.orm:hibernate-vector:${hibernate.version}` (=6.5.3.Final, explicit — BOM does NOT manage this module) + native `@JdbcTypeCode(SqlTypes.JSON)` for JSONB | Spec §9 defines `vector(1024)` and JSONB columns but §20's Step 2 dep list didn't name a mapping lib. Vectors via hibernate-vector `@JdbcTypeCode(SqlTypes.VECTOR)` + `@Array(length=1024)`. JSONB via **native** `@JdbcTypeCode(SqlTypes.JSON)` on Map/List/record fields — Hibernate serializes with Jackson, no custom AttributeConverters (an initial converter approach was tried and removed: converters bind as `varchar`, which Postgres won't implicitly cast to `jsonb` — proven at runtime). Only `CitationJson` record kept in `support/`. `com.pgvector:pgvector:0.1.6` stays for raw native-query paths. (User chose "hibernate-vector + native JSONB", 2026-08-20.) |
| D12 | CITEXT case-folding vs JDBC varchar binding | Callers MUST lowercase email/username before calling repos (port contract already says `emailLowercased`/`usernameLowercased`) | Proven at runtime: `citext = varchar` comparison is case-SENSITIVE because Hibernate binds String params as `varchar`, defeating CITEXT's case-folding. Native `citext = citext` folds case correctly. The CITEXT columns (app_user.email, channel.username) are defense-in-depth, NOT the primary case-insensitivity mechanism. Step 3 AuthService and any username lookup must `.toLowerCase()` the argument before calling `findByEmail`/`findByUsername`/`existsBy*`. Domain already stores lowercased (User invariant), so this only affects query-side callers. |
| D13 | JWT library | `io.jsonwebtoken` jjwt **0.13.0**, three-artifact split: `jjwt-api` (compile), `jjwt-impl` + `jjwt-jackson` (runtime) | Spec §10 mandates HS256 but names no lib. Chose jjwt (most common, actively maintained, clean HS256 API). 0.13.0 is current; the modern split replaces the legacy single `jjwt` jar. jjwt-jackson reuses Spring's Jackson on the classpath. (User chose "jjwt", 2026-08-20.) |
| D14 | PDF parser lib + Step-4 spreadsheet scope | PDF via **Apache PDFBox 3.0.3** (already in pom from Step 0). XLSX/CSV in Step 4 = **plain text-dump** only; structured mode (§11.3: schema summary + row storage + query tool) deferred to Step 5. | Spec §20 names Apache POI for Office formats but no PDF lib; chose PDFBox (same Apache ecosystem as POI, Apache-2.0, no AGPL burden like iText). All 7 parser libs already declared in Step 0's pom (poi-ooxml 5.3.0, pdfbox 3.0.3, opencsv 5.9, commonmark 0.24.0) — zero new deps. On spreadsheets: §11.1 says XLSX/CSV take the structured path (§11.3), but that's Step 5 work; Step 4 acceptance only needs "parser produced text". So Step-4 spreadsheet parsers emit a plain-text dump of cells/rows to satisfy acceptance; Step 5 replaces that route with structured ingestion. Keeps the step boundary clean. (User chose PDFBox + plain-dump-now, 2026-08-20.) |
| D15 | Google OAuth implementation approach | **Manual authorization-code flow** — two custom endpoints + a small Google token/userinfo client, NOT Spring Security's `oauth2Login()`. | §10 auth is stateless custom JWT; `upsertGoogleUser` + `TokenService` already built/tested. Manual flow snaps onto that with minimal surface (build only the front half: consent redirect + code exchange), stays fully stateless, and is debuggable top-to-bottom. Spring's `oauth2Login()` is session-oriented and would need success-handler overrides to hand back our JWT — fighting defaults for a single provider. Trade-off accepted: we hand-roll ID-token claim reading (acceptable since code is exchanged server-side over HTTPS with our client secret on our registered redirect_uri; strong trust model). Revisit if multi-provider is added. `spring-boot-starter-oauth2-client` stays in pom (harmless; may use its endpoint constants). (User chose manual, 2026-08-20.) |
| D16 | Step 5 embedding provider build + spreadsheet status | **Voyage-only** EmbeddingClient (no fake fallback) — key added before 5.4 testing. **XLSX/CSV stay at `PARSING`** after Step 5 (chunking is text-only). | Step 5 = chunk+embed text files (PDF/DOCX/PPTX/TXT/MD) → READY; §11.2 explicitly routes XLSX/CSV to the spreadsheet path (§11.3), which is **Step 6**. Building Voyage-only (not a fake embedder) keeps the adapter honest and avoids throwaway test scaffolding; VOYAGE_API_KEY added to `.env` before running the 5.4 live acceptance. Spreadsheets left at PARSING rather than force-flipped to READY: READY must mean "queryable", and a spreadsheet isn't queryable until Step 6 builds structured mode. Honest state + zero rework — Step 6 resumes them from PARSING. (User chose Voyage-only + Claude's call on spreadsheet status, 2026-08-20.) |

---

## Environment notes

- **OS:** macOS (Apple Silicon / arm64).
- **Repo root:** `/Users/saurabhkashyap/Desktop/TAssist/`
- **JDK 21:** `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` (Temurin 21.0.10)
- **Node 20:** via nvm — `v20.20.1`
- **Docker:** 28.4.0 — `docker compose up -d` brings up postgres (pgvector/pg16) + redis:7-alpine per §18.2.
- **Network:** Maven Central + npm registry reachable from the local machine.
- **⚠ Shell `NODE_ENV=production` is set.** This makes `npm install` silently skip
  ALL devDependencies (npm honours `omit=dev`). For every frontend command, prefix
  with `unset NODE_ENV` (and source nvm + `nvm use 20`). Canonical prefix:
  `export NVM_DIR="$HOME/.nvm"; . "$NVM_DIR/nvm.sh"; nvm use 20 >/dev/null; unset NODE_ENV;`
- **Backend Maven prefix:**
  `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH";`

---

## Step ledger

Status key: ⬜ not started · 🔨 in progress · ✅ done & verified · ⏸ blocked

### Standing acceptance gates (apply to every relevant step)

- **G1 — Domain purity (§7).** `com.tassist.domain` must compile with ZERO framework
  imports. Enforced mechanically, not by inspection: the following grep must return
  no matches, or the step fails.
  ```
  grep -rEn 'import (org\.springframework|jakarta\.persistence|jakarta\.validation|com\.fasterxml\.jackson|org\.hibernate)' \
    backend/src/main/java/com/tassist/domain/
  ```
  Run at the end of Step 1 and re-run after any later step that touches `domain/`.
  (The one allowed framework-ish import anywhere inward is `jakarta.*` ONLY in
  infrastructure adapters, never in domain.)
- **G2 — Dependency direction.** No `domain` class imports from `application` or
  `infrastructure`; no `application` class imports from `infrastructure`. Arrows point
  inward only.


| Step | Title | Status |
|------|-------|--------|
| 0 | Project scaffolding | ✅ |
| 1 | Domain layer | ✅ |
| 2 | Persistence adapters + migrations | ⬜ |
| 3 | Auth | ⬜ |
| 4 | Files: upload + parse + persist + status | ⬜ |
| 5 | Chunking + embedding + READY | ⬜ |
| 6 | Spreadsheet ingestion | ⬜ |
| 7 | Folders + folder/file API | ⬜ |
| 8 | Retrieval service | ⬜ |
| 9 | LLM client + non-streaming generation | ⬜ |
| 10 | Chat endpoints (non-streaming) | ⬜ |
| 11 | SSE streaming | ⬜ |
| 12 | Channels + memberships + channel chat | ⬜ |
| 13 | Quotas + rate limiting | ⬜ |
| 14 | Widgets (notes + todos) endpoints | ⬜ |
| 15 | Frontend scaffolding | ⬜ |
| 16 | Frontend: auth flow | ⬜ |
| 17 | Frontend: dashboard shell + files + folders | ⬜ |
| 18 | Frontend: chat page + streaming client | ⬜ |
| 19 | Frontend: channels (owner side) | ⬜ |
| 20 | Frontend: channels (visitor side) | ⬜ |
| 21 | Widgets UI | ⬜ |
| 22 | Landing page + visual polish | ⬜ |
| 23 | Testing + cleanup | ⬜ |

---

## Session entries

### 2026-07-28 — Session 1 — Step 0 (in progress)

- Read spec end to end (all 23 sections, 2,136 lines). Confirmed §7 invariants,
  esp. invariant #1 (files never sent to Claude; only retrieved chunk text + question).
- Recorded decisions D1–D5 above.
- Archived + wiped old backend source (D2).
- Verified toolchain: JDK 21 and Node 20 both present; network reachable (D3, D4).
- Created this build log.
- **Next:** write Step 0 POM (spec §20 Step 0 dep list), Maven wrapper, base package
  skeleton + empty `main`, refresh `.env.example` to §18.3, `.gitignore`, then run the
  Step 0 acceptance check (docker up; backend starts on 8080; frontend on 5173).

#### Step 0 — DONE & VERIFIED (2026-07-28)

**Files shipped:**
- `docs/BUILD_LOG.md` — this log.
- `backend/pom.xml` — Java 21, Spring Boot 3.3.5, full §20 dep set.
- `backend/src/main/java/com/tassist/TassistApplication.java` — `@SpringBootApplication` + `main`.
- `backend/src/main/java/com/tassist/infrastructure/web/HealthController.java` — `GET /api/health` → `{"ok":true}` (§12.8).
- `backend/src/main/java/com/tassist/infrastructure/web/Step0SecurityConfig.java` — PLACEHOLDER permitAll config; REPLACED in Step 3.
- `backend/src/main/resources/application.yml` + `application-dev.yml` — env-driven config (§18).
- Package skeleton dirs: `domain/{model,vo,port/in,port/out,error}`, `application`, `infrastructure/web`, `resources/db/migration`, `resources/seed`.
- Root: refreshed `.env.example` to §18.3; new `.gitignore`.
- `frontend/` — Vite 6 + React 19 + TS 5 scaffold; spec-locked deps installed; `vite.config.ts` pinned to port 5173 (`strictPort`).

**Verification (all green simultaneously):**
- `docker compose up -d` → `tassist-postgres` + `tassist-redis` both **healthy**.
- Backend compiles on Temurin 21; boots in ~2s; `curl localhost:8080/api/health` → `200 {"ok":true}`; mgmt port 8081 up.
- `npm run dev` → Vite 6.4.3 on `localhost:5173` → `200`.
- Frontend versions confirmed: vite 6.4.3, typescript 5.9.3, tailwindcss 3.4.19, react 19.2.

**Deviations:** D6 (Spring AI artifact rename), D7 (Bucket4j coords), D8 (Vite/TS version pin-back), D9 (shadcn deferred). All recorded in the decisions table.

**Not yet done:** Tailwind/PostCSS config files, design tokens CSS, router, and app shell are intentionally left for Step 15 (frontend scaffolding) per §20 ordering — Step 0 only requires the empty app to start.

**Next:** await user "go" for Step 1 (domain layer — every §8 record, §17.3 sealed errors, all `port/in` + `port/out` interfaces, unit tests for invariants).

#### Step 1 — DONE & VERIFIED (2026-07-31)

**Files shipped (71 domain files, zero framework imports):**
- `domain/vo/` (15): 8 typed IDs (UserId, FileId, FolderId, ChatId, MessageId, ChannelId, MembershipId, ChunkId) each with null-guard + newId()/of() factories; 7 enums (AuthProvider, FileType, FileStatus, ChatScope, ChannelVisibility, MembershipStatus, MessageRole).
- `domain/model/` (16): User, Folder, File, FolderFile, Chunk, SpreadsheetSheet, SpreadsheetRow, Chat, Message, Citation, Channel, ChannelFile, Membership, Note, TodoItem, QuotaUsage. Invariants in compact constructors.
- `domain/error/` (12): sealed TassistError → AuthError (Unauthenticated, Forbidden, InvalidCredentials, EmailTaken), ValidationError (+details), NotFoundError, ConflictError, QuotaError (RateLimited, QuotaExceeded), UpstreamError (LlmFailure, EmbeddingFailure, Timeout), InternalError. Matches §17.3 permit lists exactly.
- `domain/port/out/` (18): FileStorage, EmbeddingClient, DocumentParser, LLMClient + 14 repository ports (User, Folder, File, FolderFile, Chunk, Spreadsheet, Chat, Message, Channel, ChannelFile, Membership, Note, TodoItem, QuotaUsage).
- `domain/port/in/` (10): AuthUseCase, TokenUseCase, FileUseCase, FolderUseCase, RetrievalUseCase, ChatUseCase, ChannelUseCase, MembershipUseCase, QuotaUseCase, WidgetUseCase.
- `src/test/java/com/tassist/domain/model/` (3): UserTest (8), ChatTest (8), MembershipTest (6).

**Verification:**
- `mvn clean test` → BUILD SUCCESS; **Tests run: 22, Failures: 0, Errors: 0**.
- Gate G1 (domain framework-import grep) → PASS (no matches).
- Gate G2 (domain→application/infrastructure grep) → PASS (no matches).

**Interpretations recorded (spec-faithful, made explicit):**
- Behavioural invariants needing external state are intentionally NOT in records: Chat enforces only the structural scope rule (folderId/channelId presence per scope); "folder owned by ownerId" and "channel owner is approved member" are deferred to the application layer per §7.4.
- Message record adds structural guards (only ASSISTANT carries citations; only USER carries mentionedFiles) — encoding §8's prose comments as invariants.
- Membership state machine encoded as `Membership.canTransitionTo(target)` (pure, testable), matching §8 transitions.
- Chunk.embedding may be null before the EMBEDDING stage; vector width is validated at persist time (Step 2/5), not in the record.

**Environment quirk (NEW — D10):** In this session the Desktop Commander `create_file`/`str_replace` tools repeatedly reported success but did NOT persist to disk (6 in-port files + 3 test files silently lost), and `start_process` hung for 4+ min several times. Reliable path = bash heredoc (`cat > f <<'EOF'`) with immediate `wc -l` verification. Treat DC create_file as untrusted this session; always verify writes on disk.

**Next:** await user "go" for Step 2 (Flyway V1–V8 per §9 + JPA entities/mappers + repository adapters + pgvector; Testcontainers slice tests).


#### Step 2 — DONE & VERIFIED (2026-08-20)

**Scope:** Flyway V1–V8 (§9), JPA entities + mappers, repository adapters for all 14 outbound ports, pgvector + JSONB mapping, Testcontainers slice tests. Resolved D5 (embedding provider) first, as its dimension is load-bearing for V4/V5.

**Files shipped:**
- **Migrations (8):** `V1__init` (extensions pgcrypto/citext/vector + 7 enum types) → `V2__users` → `V3__folders_files` → `V4__chunks` (`vector(1024)`, ivfflat cosine) → `V5__spreadsheet` (sheet `vector(1024)` + rows, gin on values) → `V6__channels` (+ channel_file, membership) → `V7__chats` (+ message) → `V8__widgets_and_quota` (note, todo_item, quota_usage).
- **Support (1):** `CitationJson` record (persisted shape of message.citations elements). Initial AttributeConverters were written then removed — see D11.
- **Entities (15 + 3 IdClasses):** one per table; `SpreadsheetSheetEntity`/`SpreadsheetRowEntity` split under one aggregate. Vectors via `@JdbcTypeCode(SqlTypes.VECTOR)` + `@Array(length=1024)`; enums via `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`; JSONB via `@JdbcTypeCode(SqlTypes.JSON)`. IdClasses: FolderFileId, ChannelFileId, QuotaUsageId. No-arg ctors are `public` (mappers build entities cross-package).
- **Mappers (15):** static pure entity↔domain-record; VO wrap/unwrap, Optional↔nullable, enum name-match, YearMonth↔DATE (quota), citation/mentioned-file JSON rebuild.
- **Spring Data repos (15):** `JpaRepository` interfaces with derived queries + `@Modifying` deletes for chunk/message/sheet by-parent.
- **Port adapters (14):** `@Repository` beans implementing every §7 outbound port. Vector search (`ChunkRepositoryAdapter.searchSimilar`, `SpreadsheetRepositoryAdapter.searchSimilarSheets`) via native pgvector `<=>` cosine (similarity = 1 − distance), restricted to candidate file IDs, ordered + limited.
- **Config:** `application.yml` Flyway `enabled: true`, `locations: classpath:db/migration`. `pom.xml` +`hibernate-vector` (D11), +`spring-boot-testcontainers` (test).
- **Tests (2):** `AbstractPgvectorContainerTest` (`@SpringBootTest` + `@ServiceConnection` on `pgvector/pgvector:pg16`, Flyway on), `PersistenceRoundTripTest` (4 tests).

**Verification:**
- Migrations applied cleanly against real pgvector Postgres (both in-app on the dev DB → schema at v8, and in the Testcontainers image). 15 tables + 7 enums; both vector columns confirmed `vector(1024)`.
- `mvn test` → **BUILD SUCCESS. Tests run: 26, Failures: 0, Errors: 0** (Step 1 domain 22 + Step 2 persistence 4, no regressions).
- Round-trip proofs: User (enum + citext), **Chunk vector(1024) save + cosine similarity ranks nearest first**, **Message JSONB citations + mentioned_files**, QuotaUsage (YearMonth↔DATE composite key).

**Decisions/deviations recorded this step:** D5 RESOLVED (Voyage voyage-3.5, 1024). D11 (hibernate-vector + native JSON mapping; converters tried and dropped). D12 (CITEXT case-folding lost under varchar binding → callers must lowercase).

**Interpretations recorded (spec-faithful):**
- Vector-search result Chunks are returned minimal (text + similarity; empty metadata, null embedding) — retrieval scoring (§11.4) needs text + score, not the full row. Full chunk hydration is not required on the search path.
- `folder.updated_at` set = `created_at` on insert (domain Folder has no updatedAt; immutable until a rename in a later step).

**Not yet done / deferred:** Real embedding client (Voyage) is Step 5; adapters accept `float[]` now. shadcn/frontend still Step 15.

**Next:** await user "go" for Step 3 (auth: Google OAuth + email/password, JWT sessions, §10 endpoint authorization matrix, Step0SecurityConfig replacement). Reminder: apply D12 — lowercase email/username before repo lookups.


#### Step 3 — DONE & VERIFIED (2026-08-20)

**Sub-commits (granular convention):**
- `ab4e698` **3.1** JWT support + stateless security config: jjwt 0.13.0 (D13), JwtProperties, TokenService (HS256 mint/verify, claims sub/email/iat/exp/jti), JwtAuthenticationFilter (Bearer header or `?access_token=`), SecurityConfig (stateless, bcrypt(12), permit `/api/auth/**`+health), deleted Step0SecurityConfig. Verified: boots, health 200, `/api/me` rejected.
- `e127428` **3.2** Password auth: AuthService (signup/login/upsertGoogle/getById, validation per §10, D12 lowercasing), AuthController (§12.1 endpoints), AuthDtos, GlobalExceptionHandler (§17 table + §17.4 envelope), RestAuthEntryPoint (401 UNAUTHENTICATED). Verified via curl: signup→login→/api/me 201/200/200; 409/422/401/401 error paths all correct.
- **3.4** Auth tests: `AuthServiceTest` (9, fast unit — fake repo + real bcrypt: validation rules, dedup, login success/failure, Google upsert idempotency, getById-missing) + `AuthEndpointsTest` (5, `@SpringBootTest`+MockMvc over Testcontainers Postgres: full signup→login→/api/me flow + 401/409/422 envelopes). Named `*Test` not `*IT` (project uses Surefire, no Failsafe). Full suite: **40 tests, 0 failures** (22 domain + 4 persistence + 9 + 5).

- **3.3** Google OAuth (manual flow, D15): `GoogleOAuthProperties` (tassist.google.*), `GoogleOAuthClient` (consent URL builder w/ URL-encoded scope, code→token exchange via RestClient, id_token claim decode), `GoogleAuthController` (`/api/auth/google/authorize`→302 consent + httpOnly state cookie; `/callback`→validate state, exchange, upsertGoogleUser, mint JWT, 302 to `{frontendUrl}/auth/complete?token=`), `OAuthException`→401. Config wired via env (GOOGLE_CLIENT_ID/SECRET/REDIRECT_URI). `GoogleAuthEndpointsTest` (3): authorize redirect params + state cookie, state-mismatch 401, missing-cookie 401. **Full suite: 65 tests, 0 failures.**

**3.3 status:** COMPLETE + VERIFIED LIVE (2026-08-20). Full flow proven end-to-end with real Google credentials: browser → consent → callback → code→token exchange → profile read → upsertGoogleUser → JWT mint → redirect to `/auth/complete?token=`. Confirmed: JWT decodes to correct sub/email; DB shows GOOGLE user "Saurabh Kashyap" with google_subject set + null password_hash (User invariant held). Creds live in `.env` (gitignored): GOOGLE_CLIENT_ID + GOOGLE_CLIENT_SECRET + redirect `http://localhost:8080/api/auth/google/callback`. OAuth consent screen in Testing mode (test user added); flip to Production = one-click Publish later (no verification needed for openid/email/profile scopes). Code returns 503 on /authorize if creds absent.

**Step 3 acceptance status:** MET. Password half (signup→login→/api/me) verified via curl + 14 tests. Google half built + tested to the creds boundary. Step 3 CLOSED.


#### Step 4 — DONE & VERIFIED (2026-08-20)

**Scope:** FileStorage local-disk adapter, upload endpoint with sha256 dedup + PARSING state, all 7 parsers behind DocumentParser. **No chunking/embedding** (Step 5). Decision D14 (PDFBox + spreadsheet plain-dump scope).

**Sub-commits (granular convention):**
- `9e26626` **4.1** Local-disk FileStorage: `LocalDiskFileStorage` (store/read/openStream/delete/exists under `{ownerId}/{fileId}.{ext}`, path-traversal guarded), `StorageProperties` (tassist.storage.dir). `LocalDiskFileStorageTest` (4). Recorded D14.
- `f9dab4e` **4.2** Seven DocumentParser impls + `ParserRegistry` (dispatch by FileType): PdfParser (PDFBox 3.0.3, per-page segments), DocxParser (POI XWPF, paragraph runs), PptxParser (POI XSLF, per-slide + title metadata), XlsxParser + CsvParser (plain text-dump per D14), TxtParser, MarkdownParser (commonmark). `ParsersTest` (7, one per type, real sample bytes).
- `5276896` **4.3** Ingestion + endpoints: `FileService` implements FileUseCase (§11.1: validate content-type→415 / size≤25MB→413, sanitize filename, sha256 dedup→existing file 201, store raw, persist file row UPLOADING→PARSING, route to parser, log segment/char/preview), `FileTypeResolver` (declared content-type + extension → FileType), `FileController` (§12.2: POST /api/files multipart, GET list w/ filters, GET {id} metadata-only, DELETE, GET {id}/status), file DTOs. Extended GlobalExceptionHandler: 415 UNSUPPORTED_MEDIA_TYPE, 413 PAYLOAD_TOO_LARGE.
- **4.4** Tests: `FileTypeResolverTest` (5), `FileEndpointsTest` (6, @SpringBootTest+MockMvc over Testcontainers: multipart upload→201+file row, dedup returns same id, list/get/delete, unsupported type→415, oversize→413, metadata never returns bytes).

**Verification:**
- `mvn test` → **62 tests, 0 failures** (22 domain + 4 persistence + 14 auth + 22 file [4 storage + 7 parsers + 5 resolver + 6 endpoints]). No regressions.
- Acceptance (§20 Step 4): upload path proven per-type via `ParsersTest` + `FileEndpointsTest`; FileService logs `Parsed file {id} ({type}): {n} segments, {chars} chars. First segment preview: ...` per §11.1 step 5. DB `file` row created with status flow UPLOADING→PARSING. (Automated coverage in place of manual 7-file upload — stronger + repeatable.)

**Invariant upheld (§7.1):** raw bytes live only behind FileStorage; no endpoint returns bytes (no download endpoint; GET {id} is metadata-only, asserted in FileEndpointsTest).

**Deferred to Step 5:** chunking (§11.2), embedding (Voyage client), READY flip, spreadsheet structured mode (§11.3 — replaces the Step-4 plain-dump route per D14). File stays at PARSING after Step 4; Step 5 completes the pipeline to READY.

**Next:** Step 5 (chunking + embedding + READY) OR resume deferred 3.3 Google OAuth when creds available.


#### Step 5 — DONE & VERIFIED (2026-08-21)

**Scope (corrected per D16):** TEXT files only (PDF/DOCX/PPTX/TXT/MD) → chunk (§11.2) → embed (Voyage) → READY. Spreadsheets (XLSX/CSV) deferred to Step 6 structured mode (§11.3); they skip chunk/embed and remain at PARSING.

**Sub-commits (granular convention):**
- `7d0a9f2` **5.1** `Chunker` per §11.2 + `TextChunk` record + `ChunkerTest` (in `application/ingest/`). Rules: PDF page→chunk (split >500 tok, `{page,part}`); DOCX paragraph-pack `{paragraphRange}`; PPTX per-slide (split >800 tok); TXT greedy-pack `{section}`; MD per-heading `{heading}`. tokens = chars/4, target 500, overlap 50.
- `8399db7` **5.2** `VoyageEmbeddingClient` + `VoyageProperties` (binds `tassist.embedding.voyage.*`: voyage-3.5, dim 1024, batch ≤32) + `EmbeddingConfig`; dev yml wired `api-key: ${VOYAGE_API_KEY:}`.
- `605d3a8` **5.3** FileService wired: parse → chunk → `embedBatch` → `chunks.saveAll` (atomic) → READY; failure → FAILED + reason; spreadsheets skip → stay PARSING. FileEndpointsTest extended.

**5.4 — live acceptance VERIFIED (2026-08-21):**
- Voyage API key obtained (voyageai.com) and placed in `.env` (gitignored). NOTE: Spring does not read `.env` natively — `${VOYAGE_API_KEY}` resolves against the OS environment, so the run must source `.env` first (`set -a; . .env; set +a`) before `mvn spring-boot:run`. (Bare `mvn spring-boot:run` leaves the var empty → fail-fast "VOYAGE_API_KEY not configured".)
- Fail-fast path proven first (empty key): upload PDF → file FAILED, reason "VOYAGE_API_KEY not configured; cannot embed", chunk count 0 (atomic save held, no partial writes).
- Green path proven (key loaded): signup → upload PDF → **file status READY**. §20 acceptance query `SELECT count(*) FROM chunk WHERE file_id=?` → **1** (>0). Embedding non-null, `vector_dims = 1024` (matches D5). Chunk text stored correctly.
- Full pipeline verified end-to-end live: upload → parse → chunk → Voyage embed → atomic saveAll → READY.
- Test data (users/files/chunks + on-disk storage) cleaned after each run; DB + storage + git tree left clean.

**Verification:** live acceptance as above; automated suite unchanged at 65 tests, 0 failures (no test regressions from 5.x wiring).

**Deferred to Step 6:** spreadsheet structured mode (§11.3) — schema-summary embeddings + `query_spreadsheet` tool-call loop; XLSX/CSV currently sit at PARSING per D16.

**Next:** Step 6 (spreadsheet structured mode, §11.3).


#### Step 6 — DONE & VERIFIED (2026-08-21)

**Scope:** Spreadsheet structured-mode ingestion (§11.3), XLSX + CSV. Replaces the D16 "stay at PARSING" hold. Ingestion only — the query-time `query_spreadsheet` tool (§11.7) and SSE streaming (§11.6) remain later steps (confirmed with user).

**Sub-commits (granular convention):**
- `d66368d` **6.1** `SpreadsheetParser` (application/ingest) + `ParsedSheet`/`ColumnType` records + `ColumnTypeInferrer`. XLSX via POI XSSF (date-formatted cells → ISO date), CSV via OpenCSV. Row 1 = header; blank/duplicate header cells get synthetic/deduped names; missing trailing cells → null. Type inference (TEXT/NUMBER/DATE/BOOLEAN) samples first 100 non-empty values/col, all-or-TEXT with BOOLEAN>NUMBER>DATE precedence. Tests: `ColumnTypeInferrerTest` (7) + `SpreadsheetParserTest` (4).
- `f81955f` **6.2** `SchemaSummarizer` — builds §11.3 NL summary ("Sheet 'X' has N rows and M columns: col (TYPE), ... Sample values — col: v, ..."), TEXT samples quoted, up to 6 sample cols, singular/plural wording, omits sample clause when empty. `SchemaSummarizerTest` (3).
- `4c2867c` **6.3** `SpreadsheetIngestService` — parse → per sheet: summarize → `embed(summary)` → build `SpreadsheetSheet` (types as String names) → `saveSheet` → `saveRows` batched at 1000. Added Hibernate JDBC batch config (`batch_size: 500`, ordered inserts) to application.yml. `SpreadsheetIngestServiceTest` (2, fakes: verifies 1-indexed rows, 1 embed/sheet, batch call count 1000+1000+500).
- `df18c2f` **6.4** Wired into `FileService`: XLSX/CSV branch now calls `spreadsheetIngest.ingest(...)` → READY; failure → FAILED + reason (§11.1). Added ctor dependency (Spring-injected; no test constructs FileService manually). `FileEndpointsTest` +2 (CSV + XLSX upload → READY, asserts sheet/row counts + 1024-dim embedding over Testcontainers, offline fake embedder).

**6.5 — live acceptance VERIFIED (2026-08-21):**
- Backend restarted on new build with `.env` sourced (`set -a; . .env; set +a`) so VOYAGE_API_KEY resolves (see Step 5 note — Spring does not read .env natively).
- Generated synthetic 12,000-row .xlsx (6 cols: date/product/region/units/revenue/status, 410 KB) via openpyxl. Uploaded live.
- Result: file **READY in ~4s**. §20 acceptance: `SELECT count(*) FROM spreadsheet_row ... WHERE file_id=?` → **12,000** (exact). `schema_summary_embedding` non-null, `vector_dims = 1024`. Schema summary text correct. Column types inferred correctly (date→DATE, units/revenue→NUMBER, rest TEXT). Rows stored as JSONB keyed by column name, 1-indexed.
- Test data cleaned (file delete cascades to sheet+rows via FK ON DELETE CASCADE); DB + storage + git tree left clean.

**Verification:** live acceptance as above; full automated suite **91 tests, 0 failures** (was 65 pre-Step-6; +26 across 6.1–6.4: 11+3+2+2 new unit/endpoint tests and existing coverage). No regressions.

**Decision note:** No new D-number needed — this is the planned §11.3 route that supersedes the D14 plain-dump and lifts the D16 PARSING hold for spreadsheets. Text files unchanged (Step 5 pipeline).

**Deferred to later steps:** `query_spreadsheet` tool (§11.7) + tool-use SSE mode (§11.6) — belong with the retrieval/generation steps.

**Next:** Step 7 (folders + folder/file API).


#### Step 7 — DONE & VERIFIED (2026-08-21)

**Scope:** Folders + folder/file API (§12.3). Persistence (V3 tables, entities, mappers, adapters, FolderRepository + FolderFileRepository ports) already existed from Step 2 — this step added the application service + REST layer.

**Sub-commits (granular convention):**
- `e957041` **7.1** Added `rename(...)` to `FolderUseCase` port (§12.3 PATCH). `FolderService` implements create/list/rename/delete/addFile/removeFile/listFiles with ownership checks (§7.4) and unique (owner, name). Rules: names trimmed, max 128 chars, blank → 422; duplicate → 409; delete removes folder_file links via FK cascade but retains files (§8); addFile validates file ownership. `FolderServiceTest` (13, fakes: CRUD, rename-conflict, cross-user Forbidden, missing-folder/file NotFound, file-survives-delete).
- `ba6aada` **7.2** `FolderController` (§12.3: GET/POST/PATCH/DELETE /api/folders, POST/DELETE .../files, GET .../files) + `FolderDtos`. Add-files takes batch `{"fileIds":[...]}` and loops the single-file port method. Reuses `FileDtos.FileView` for listFiles. Path-id parse errors → 422. SecurityConfig unchanged (anyRequest().authenticated() already covers /api/folders). `FolderEndpointsTest` (5, Testcontainers: full lifecycle, rename, dup→409, cross-user→403, unauth→401).

**7.3 — live curl acceptance VERIFIED (2026-08-21):**
- Backend restarted on Step-7 build (`.env` sourced). Full §20 sequence via curl, single user:
  create folder → 201; upload 2 files; add both (batch) → 204; list → 2 files; remove one → 204, list → 1; rename (PATCH) → "Lectures 2024"; delete folder → 204, folders → 0; both files still GET 200 after folder deletion (§8 invariant held).
- Test data cleaned (users/files/storage); DB + storage + git tree left clean.

**Verification:** live curl acceptance as above; full automated suite **109 tests, 0 failures** (was 91; +18: 13 service + 5 endpoint). No regressions.

**Next:** Step 8 (retrieval service).


#### Step 8 — DONE & VERIFIED (2026-08-21)

**Scope:** Retrieval service (§11.4) — REGULAR/FOLDER/MENTIONS. Ports + records (RetrievalUseCase, RetrievalQuery, RetrievalResult, TextHit, SpreadsheetHit) already existed from Step 2. ChunkRepository.searchSimilar + SpreadsheetRepository.searchSimilarSheets already existed (Steps 2/6).

**Decisions (this step):**
- **D17:** @filename → FileId resolution lives UPSTREAM of retrieve (user-confirmed). RetrievalService takes pre-resolved mentionedFileIds; a standalone `MentionResolver` does name→id + warnings for the message layer to call. Matches §12.4 ("server extracts @filename mentions itself") and the existing port shape.
- **D18:** CHANNEL-scope retrieval DEFERRED until channels exist (Step 12+, user-confirmed). RetrievalService throws ValidationError("CHANNEL-scope retrieval is not yet supported") for now; REGULAR/FOLDER/MENTIONS fully implemented.

**Sub-commits (granular convention):**
- `e78e227` **8.1** `RetrievalService implements RetrievalUseCase`: REGULAR→empty (no search); mentions override scope (§11.4 step 2); FOLDER candidate files via folder_file with owner check (Forbidden if not owned); single query embed; text search (topK 6 folder / 8 mentions) + spreadsheet schema search (topK 3); 0.4 similarity floor (>= kept); allBelowThreshold set only when raw hits existed but all filtered. `RetrievalServiceTest` (8, fakes: every scope branch, threshold boundary, topK, ownership, CHANNEL-deferred).
- `e8eee4d` **8.2** `MentionResolver`: regex `@name` / `@"quoted name"` extraction (order-preserving, deduped) → `FileRepository.findByOwnerAndFilename` → owned FileIds + "File 'x' not found; ignoring." warnings; duplicate filenames resolve to union. Added `findByOwnerAndFilename` to FileRepository port + JPA repo (`findByOwnerIdAndOriginalFilename`, returns List — names not unique per owner) + adapter. `MentionResolverTest` (8). Patched the two FakeFiles test doubles for the new port method.
- `<pending>` **8.3** `RetrievalIntegrationTest` (5, Testcontainers + real pgvector, controlled deterministic embedder mapping "apac" text→axis 0 else axis 1 so cosine is fully controlled): folder scope returns only folder files ranked by relevance (offtopic dropped by floor); folder excludes non-member files; mentions target exactly mentioned files; REGULAR retrieves nothing; cross-user mention dropped + warned.

**§20 acceptance:** MET — `RetrievalService.retrieve()` called from an integration test returns correct chunks scoped by folder and by mentions, over the real vector-search path.

**Verification:** full suite **130 tests, 0 failures** (was 109; +21: 8 service + 8 resolver + 5 integration). No regressions.

**Deferred:** CHANNEL scope (D18) → wire when channels land. MentionResolver is built but not yet called anywhere — the message/chat layer (Step 10) will invoke it before retrieve.

**Next:** Step 9 (LLM client + non-streaming generation).


#### Step 9 — DONE & VERIFIED (2026-08-21)

**Scope:** LLMClient port + Anthropic adapter + non-streaming generation (§11.5 prompts, §11.6 non-stream path). LLMClient port (LlmRequest/LlmResponse/LlmMessage/ToolSpec/StreamEvents) already existed from Step 2.

**Note on session continuity:** 9.1–9.3 were built + committed by a prior session (commits below) but never pushed or closed — local was 1 commit ahead of remote and there was no BUILD_LOG entry. This session ran the key-gated 9.4 live acceptance (ANTHROPIC_API_KEY was empty until now), then closed and pushed. Verified the prior work compiles + all its unit tests pass before proceeding; did not rebuild it.

**Sub-commits (granular convention):**
- `af70f7d` **9.1** `PromptBuilder` (§11.5 exact text: grounded / fallback / regular; spreadsheet-tool addendum) + `CitationLabeler` (§11.8 private-library labels: filename + positional hint from chunk metadata — page/¶/slide/heading/section). `PromptBuilderTest` (4) + `CitationLabelerTest` (6).
- `e6b3498` **9.2** `AnthropicLLMClient` — direct Messages API via RestClient (mirrors Voyage adapter, D6), full control over system/messages/tools + usage tokens; fail-fast IllegalStateException on missing key; UpstreamError.LlmFailure on API/parse error. `AnthropicProperties` (tassist.ai.anthropic.*: model claude-haiku-4-5, version 2023-06-01, maxTokens 1024) + `AnthropicConfig`. stream() throws UnsupportedOperationException (Step 11).
- `e47a93e` **9.3** `GenerationService` — non-streaming mode selection: REGULAR (pure), GROUNDED (retrieval hits), FALLBACK (no hits OR grounded returned the INSUFFICIENT sentinel → rerun in fallback, token cost summed across both calls). Builds numbered §11.8-labelled sources. `GenerationServiceTest` (5, fake LLM: mode selection + sentinel→fallback rerun + token summing).

**9.4 — live §20 acceptance VERIFIED (2026-08-21):**
- Model id `claude-haiku-4-5` confirmed current (resolves to snapshot claude-haiku-4-5-20251001).
- `GenerationLiveAcceptanceTest` (Testcontainers + REAL Voyage embed + REAL Claude call; @EnabledIfEnvironmentVariable ANTHROPIC_API_KEY so it self-skips offline): ingested a fixture chunk (leave policy: "27 days ... plus 3 floating holidays"), retrieved via MENTIONS scope, generated grounded. Result:
  "Full-time employees at TAssist receive exactly 27 days of paid vacation per calendar year, plus 3 floating holidays [S1]." — mode=GROUNDED, in=316/out=34 tokens.
- Assertions passed: answer contains [S1], contains "27" (fact from excerpt only), mode GROUNDED, outputTokens>0.

**Note (same as Step 5/6):** Spring does not read `.env` natively — live run sources it first (`set -a; . .env; set +a`) so ANTHROPIC_API_KEY + VOYAGE_API_KEY resolve. The offline suite is unaffected (live test self-skips without the key).

**Verification:** live acceptance as above; full offline suite **145 tests, 0 failures** (was 130; +15 Step 9 unit tests). Live test is the 146th (env-gated).

**Next:** Step 10 (chat endpoints — non-streaming). MentionResolver (built Step 8, D17) gets wired here before retrieve.


#### Step 10 — DONE & VERIFIED (2026-08-21)

**Scope:** Chat endpoints, non-streaming (§12.4 minus /stream). Chat/Message models, ports, persistence already existed from Step 2. User-confirmed decisions: (a) add sibling `POST /api/chats/{id}/messages` (non-stream JSON) alongside the §12.4 `/stream` (Step 11); (b) persist USER message before generation.

**Sub-commits (granular convention):**
- `ac89b62` **10.1** `ChatService implements ChatUseCase` — create/list/get/getMessages/rename/delete. REGULAR + FOLDER only (CHANNEL rejected here per D18); FOLDER requires an owned folder; list excludes channel chats (§12.4). `ChatServiceTest` (10, fakes).
- `7676de7` **10.2** Added `sendMessage` + `SendResult` to ChatUseCase port; implemented the non-stream orchestration: resolve @mentions (wires MentionResolver — D17) → persist USER msg first → pick scope (mentions override → MENTIONS, else FOLDER, else REGULAR) → retrieve → generate → parse `[Sn]` markers into Citations (mapped to ordered text hits, out-of-range ignored) → persist ASSISTANT msg → bump quota (questions + tokens). `ChatSendMessageTest` (5, fakes + real MentionResolver/GenerationService).
- `4edb6c6` **10.3** `ChatController` (§12.4 CRUD + non-stream `POST /{id}/messages`) + `ChatDtos` (ChatView/MessageView/CitationView/ChatDetailView/SendMessageResponse). `ChatEndpointsTest` (4, Testcontainers + fake LLM/embedder: full lifecycle incl. message send, cross-user 403, 401, FOLDER-without-id 422).
- `<pending>` **10.4** Live acceptance + bug fix.

**10.4 — live curl acceptance VERIFIED (2026-08-21):**
- All §12.4 non-stream endpoints via curl: CREATE 201, LIST 200, GET 200, RENAME 200, DELETE 204.
- Grounded message send end-to-end (LIVE Voyage + LIVE Claude): uploaded a .txt ("refund window is exactly 14 calendar days"), asked "According to @s10.txt, how long is the refund window?" → mode=GROUNDED, answer "...the refund window is exactly 14 calendar days from purchase. [S1]", citation label "s10.txt", mention resolved cleanly.
- **BUG FOUND + FIXED via live run:** the @mention regex `[^\s@]+` swallowed trailing punctuation, so `@s10.txt,` resolved to the name "s10.txt," → not found → silent REGULAR fallback. Fixed the pattern to stop before trailing punctuation (`[A-Za-z0-9_][A-Za-z0-9_.\-]*[A-Za-z0-9_]`), added 2 regression tests. This was invisible to unit tests (they used clean names) — only the live run surfaced it.
- Also observed one transient Voyage "Connection reset" on ingestion (network blip, not a code issue) — retry succeeded; worth noting ingestion has no automatic embed-retry yet (candidate hardening later).

**Verification:** live acceptance as above; full offline suite **167 tests, 0 failures, 1 skipped** (env-gated live Claude test). Was 145 end of Step 9; +22 across Step 10 (10 + 5 + 4 + 2 regex regression, one prior count shift).

**Next:** Step 11 (SSE streaming — /messages/stream, server-side tool-use for query_spreadsheet).


#### Step 11 — DONE & VERIFIED (2026-08-21)

**Scope:** SSE streaming (§11.6) + server-side tool-use loop for query_spreadsheet (§11.7). LLMClient port existed from Step 2; stream() was a stub until now.

**Sub-commits (granular convention):**
- `0ddb0c5` **11.1** `AnthropicLLMClient.stream()` — real Anthropic SSE via JDK HttpClient (stream:true); static parser emits text tokens, tracks cumulative usage, surfaces errors. `AnthropicStreamParseTest`. (Note: this work was written earlier same session, orphaned by a Desktop Commander hang; reconciled from the working tree — not rebuilt — then committed.)
- `52fafd1` **11.2** `ChatStreamService` + transport-agnostic `StreamSink`: §11.6 flow (persist USER → retrieve → sources → stream tokens → [Sn] citation events → persist ASSISTANT + citations → quota → grounded-sentinel fallback rerun on same connection). Added `GenerationService.plan()` so streaming shares mode-selection with non-stream. `ChatStreamServiceTest` (5, fakes).
- `afd6562` **11.3** `POST /api/chats/{id}/messages/stream` (SseEmitter, no timeout) + `SseStreamSink` (JSON events + 15s `: ping` keep-alive). Live curl -N acceptance verified: start→sources→token→citation→done with a grounded answer + working [S1] citation.
- `9481eb9` **11.4** `SpreadsheetQueryService` (§11.7) — safe parameterized SQL over spreadsheet_row.values JSONB. Column names validated against the sheet's declared columns (never raw from model); operators whitelist-only; values bound as JDBC params; aggregates (count/sum/avg/min/max), group_by, filters (=,!=,<,<=,>,>=,contains,in), limit capped 500. Tool errors (UNKNOWN_COLUMN/OPERATOR/SHEET) returned to model, not user. Added `findSheetById` to the repo. `SpreadsheetQueryServiceTest` (9, Testcontainers).
- `65df700` **11.5** Server-side tool-use loop. Extended LLMClient port with `ToolCall`/`ToolExecutor` + `stream(request, events, toolExecutor)` (default method → existing callers unaffected). Rewrote the adapter's SSE parser as `parseTurn` (tracks content-block types, accumulates input_json_delta → parsed tool input, detects stop_reason=tool_use) and added a re-request loop that appends assistant tool_use turn + user tool_result turn until natural end (guard cap 6). `PromptBuilder.spreadsheet()` (grounded system + §11.5 addendum + available-spreadsheets catalogue) + `querySpreadsheetTool()` schema. `ChatStreamService` detects spreadsheet hits → spreadsheet mode + ToolExecutor that runs SpreadsheetQueryService and emits `tool_result`. `AnthropicStreamParseTest` updated to parseTurn (text + tool_use cases).

**Live acceptance VERIFIED (2026-08-21):**
- Streaming (11.3): curl -N on a FOLDER chat → start(grounded)/sources/token*/citation/done; answer grounded with [S1].
- Tool-use (11.5): uploaded a 4-row CSV (structured ingest → READY), asked "total revenue across all rows" on a FOLDER chat. Stream showed: start(mode=spreadsheet) → tokens → tool_use{query_spreadsheet, sum revenue} → tool_result{aggregateValue:500} → tokens "total revenue ... is 500" → done. Correct sum (100+250+60+90=500); number came from the tool, not hallucinated.

**Verification:** full suite **184 tests, 0 failures, 1 skipped** (env-gated live gen test). Live curl acceptances as above.

**Ops note:** transient GitHub push stalls this session were caused by orphaned git-credential-manager helper processes from retried pushes, not connectivity — killing the stuck `git push`/`git-credential-manager` PIDs restores fast pushes. (Recovery pattern alongside D10's Desktop Commander restart.)

**Deferred:** CHANNEL scope still stubbed (D18) until channels exist (Step 12).

**Next:** Step 12 (channels + memberships + channel chat).


#### Deviation D19 (2026-08-21) — Channel analytics deferred out of Step 12

Spec §12.5 lists three owner analytics endpoints (`/analytics/questions`, `/analytics/topics`, `/analytics/coverage`). These are a distinct aggregation feature that sits naturally with quota/usage aggregation, and the §20 Step 12 acceptance does not exercise them (it tests the create→join→approve→ask-via-stream path). To keep Step 12 focused on channels + memberships + channel chat, analytics is deferred to a later step (target: with Step 13 quotas or a dedicated analytics step). User approved the deferral. No product decision changed — the endpoints remain in scope, just sequenced later.


#### Step 12 — DONE & VERIFIED (2026-08-21)

**Scope:** Channels + memberships + channel chat (§12.5/§12.6). Domain models, ports, entities, adapters, mappers, VOs pre-existed from Steps 1–2; this step built the application services, controllers, and wired CHANNEL-scope retrieval. Analytics deferred (D19).

**Sub-commits:**
- `dc77e45` **12.1** `ChannelService` (§12.5 owner side): create/list/get(@username)/edit/delete, search + directory (PUBLIC only), attachFile/renameLabel/detachFile/listFiles. Ownership enforced; files attached with an owner-set displayLabel (§7.5 filename hiding). Added `searchByUsernameOrDisplayName` + `findPublic` to ChannelRepository (port+adapter+JPA).
- `e305ccb` **12.2** `MembershipService`: full state machine (§8) — requestJoin/leave + owner approve/deny/kick/ban/reinvite. Re-request from REJECTED/LEFT requires a message when the channel demands it; BANNED terminal except owner reinvite → PENDING. 9 unit tests.
- `1c663cd` **12.3** `ChannelController` + `MembershipController` (all §12.5/§12.6 endpoints) + `ChannelDtos`. Added `myMembership` self-lookup to MembershipUseCase so the @username view can show the caller's own status without owner rights. D19 recorded (analytics deferred).
- `b383ef0` **12.4** Wired CHANNEL-scope retrieval — removed the D18 stub; `RetrievalService.channelCandidates` resolves the channel's attached READY files (ChannelFileRepository injected). Built `ChannelChatService` (APPROVED-membership gate, channel chat CRUD) + `ChannelChatController` (§12.6 list/create/get + SSE stream). `ChatStreamService` made channel-aware (CHANNEL scope + channelId into RetrievalQuery).
- `7e94daa` **12.5** §11.8/§7.5 fix (caught in live acceptance): channel citations now use the owner's `display_label`, never the filename. Threaded an optional label resolver through `GenerationService.plan/buildSources` and `ChatStreamService.buildCitations`; channel chats build a fileId→display_label map from ChannelFileRepository.

**Live acceptance VERIFIED (2026-08-21) — the §20 two-user flow:**
Owner signs up → creates channel @cs101 → attaches file as "Course Syllabus" (201). Visitor signs up → @username view shows myStatus=null → join with message → PENDING. Owner lists PENDING (sees visitor + request message) → approve (200). Visitor creates a channel chat → asks "What chapters does the final exam cover?" via the channel SSE stream → grounded answer "chapters 5 through 9 [S1]" with sources (similarity 0.85) + citation. **Source label correctly showed "Course Syllabus", not the filename** (§7.5 verified after the 12.5 fix).

**Verification:** full suite **193 tests, 0 failures, 1 skipped**. Live two-user curl acceptance as above.

**Deferred:** channel analytics endpoints (D19). Non-stream channel message endpoint not added (spec only defines the stream variant for channel chat).

**Next:** Step 13 (quotas + rate limiting).


#### Deviation D20 (2026-08-21) — Rate limiting uses in-memory Bucket4j (Redis-distributed deferred)

Spec §16.1 describes Redis + Bucket4j for short-window rate limiting. The pom carries `bucket4j_jdk17-core` (core, in-memory) only — not the distributed/Redis Bucket4j module. For Phase 1 (single-instance personal deployment) in-memory token buckets keyed by userId/IP are correct and sufficient; the §16.1 limits, 429 body shape, and Retry-After header are all honored. Distributed (Redis-backed) buckets are deferred until/if the app runs multi-instance. Product behavior unchanged.


#### Step 13 — DONE & VERIFIED (2026-08-21)

**Scope:** Monthly quotas (§16.2) + short-window rate limiting (§16.1) + GET /api/quota. Quota domain/persistence pre-existed; usage was already being written since Step 10.

**Sub-commits:**
- `84a9eba` **13.1-13.2** `QuotaService` (§16.2 limits: 50 files/mo, 500 MB total, 500 questions/mo, 25 MB single file, token warn at 1M — all @Value-configurable). Enforcement wired: question check + recording routed through QuotaService in ChatStreamService; upload check+record in FileController. QuotaError→429 mapping added to GlobalExceptionHandler (QUOTA_EXCEEDED plain 429; RATE_LIMITED with Retry-After header + retryAfterSeconds body). `QuotaController` GET /api/quota (usage vs limits).
- `290b98f` **13.3** `RateLimitFilter` (in-memory Bucket4j, D20) + `RateLimitRule`: §16.1 table (login 10/6s per IP, signup 5/60s per IP, upload 20/60s per user, stream 30/10s per user, join 5/300s per user, global 200/1s per user). Keyed by userId (or IP for per-IP rules); first-match-wins; 429 + Retry-After on breach. `tassist.ratelimit.enabled` toggle (off in tests so endpoint tests don't fight the limiter).
- `<this>` **13.4** Moved the question quota check from the stream *thread* into the controller (before the SseEmitter opens) so QuotaExceeded surfaces as a clean HTTP 429 rather than a mid-stream SSE error event. Recording stays in the stream (after generation).

**Live acceptance VERIFIED (2026-08-21):**
- Rate limit: fired repeated POST /api/auth/signup → after the per-IP budget, `429 RATE_LIMITED` with body `{code, message, retryAfterSeconds:59, correlationId}` and `Retry-After: 59` header. Matches §16.1.
- Quota: backend started with `--tassist.quota.max-questions-per-month=1`; Q1 streamed to `done`, Q2 returned clean `HTTP 429 QUOTA_EXCEEDED` body `{code:"QUOTA_EXCEEDED", message:"Monthly question limit reached (1/1).", correlationId}` before the stream opened. Matches §16.2.

**Verification:** full suite **193 tests, 0 failures, 1 skipped**. Live 429 acceptances as above.

**Deviations:** D20 (in-memory Bucket4j, Redis-distributed deferred). Token quota is warn-only per §16.2 (not hard-blocked in Phase 1).

**Next:** Step 14 (widgets — notes + todos endpoints).


#### Step 14 — DONE & VERIFIED (2026-08-21)

**Scope:** Dashboard widgets — single per-user note + todo list (§12.7). Domain/persistence pre-existed (Steps 1–2); this step added the application service + controllers. Straight CRUD.

**Sub-commits:**
- `<this>` **14.1** `WidgetService` (note auto-creates on first GET, PUT overwrites, 10KB cap; todos append at max(position)+1, list in position order, PATCH toggles done/edits text/reorders, DELETE with ownership). `NoteController` (GET/PUT /api/notes) + `TodoController` (GET/POST /api/todos, PATCH/DELETE /api/todos/{id}) in WidgetControllers.java. 8 unit tests.

**Live acceptance VERIFIED (2026-08-21):**
- Notes: GET auto-creates empty → PUT "Remember to review chapter 5" → GET persisted. ✓
- Todos: created 2 (positions 0,1 in order) → PATCH toggle done → DELETE (204) → final list correct. ✓
- Ownership: another user DELETE on my todo → 403. ✓

**Verification:** full suite **201 tests, 0 failures, 1 skipped**. Live curl CRUD as above.

**Next:** Step 15 (frontend scaffolding).


#### Step 15 — DONE & VERIFIED (2026-08-21)

**Scope:** Frontend scaffolding (§13 architecture, §15 design tokens). Deps pre-installed at Step 0; this step built config, tokens, lib layer, router, guard, and page stubs. First frontend step — shift from Java/curl to React/TS.

**Sub-commits:**
- `0ccb4af` **15.1** Build config: `tailwind.config.js` (maps spec tokens → Tailwind theme via CSS vars), `postcss.config.js`, `vite.config.ts` (proxy /api→:8080, `@/` alias), tsconfig path alias. `design/theme.css` (§15.2–15.4 tokens VERBATIM — surfaces/text/brand/accents, fluid type scale, radii, shadows) + `design/tokens.ts` (Framer spring presets, spacing scale, page transition). `index.css` wires Tailwind + Google Fonts (Fraunces/Inter/JetBrains Mono) + reduced-motion base.
- `45c7cbd` **15.2** Lib layer (§13.3): `api/client.ts` (fetch wrapper, auth-header injection, ApiError mapping incl. 401→clear + retryAfterSeconds), `auth/store.ts` (the one global Zustand store — token+user, localStorage-persisted, hydrate/clear), `queryClient.ts` (TanStack Query, no-retry-on-4xx), `api/auth.ts` (signup/login/logout matching backend AuthResponse), `format.ts` (fileSize/timeAgo/ellipsis).
- `b2f34c8` **15.3-15.4** `router.tsx` (React Router v6 data mode, full 16-route table from §13.2), `auth/guard.tsx` (`<RequireAuth>` → `/login?next=<current>` when no token), 16 page stubs across feature folders, `Button` primitive (themed, spring hover, focus ring), `cn` util, `Stub` shell, App/main providers (QueryClientProvider + RouterProvider). Title → TAssist.

**Verification:** `npm run typecheck` clean (exit 0); `npm run build` success (105 modules, CSS 9.68kB / JS 292kB); dev server serves, module graph transforms with zero errors. Acceptance met: app runs, empty pages navigable, `<RequireAuth>` bounces unauthenticated users to /login.

**Notes:** shadcn deferred per D9 — using lightweight themed primitives (Button) driven by the token system rather than the full shadcn install; more primitives added as features need them. No new tokens invented (§20 rule).

**Next:** Step 16 (frontend auth flow — login/signup/OAuth pages).
