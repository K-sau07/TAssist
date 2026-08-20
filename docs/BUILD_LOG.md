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


#### Step 3 — IN PROGRESS (2026-08-20)

**Sub-commits (granular convention):**
- `ab4e698` **3.1** JWT support + stateless security config: jjwt 0.13.0 (D13), JwtProperties, TokenService (HS256 mint/verify, claims sub/email/iat/exp/jti), JwtAuthenticationFilter (Bearer header or `?access_token=`), SecurityConfig (stateless, bcrypt(12), permit `/api/auth/**`+health), deleted Step0SecurityConfig. Verified: boots, health 200, `/api/me` rejected.
- `e127428` **3.2** Password auth: AuthService (signup/login/upsertGoogle/getById, validation per §10, D12 lowercasing), AuthController (§12.1 endpoints), AuthDtos, GlobalExceptionHandler (§17 table + §17.4 envelope), RestAuthEntryPoint (401 UNAUTHENTICATED). Verified via curl: signup→login→/api/me 201/200/200; 409/422/401/401 error paths all correct.
- **3.4** Auth tests: `AuthServiceTest` (9, fast unit — fake repo + real bcrypt: validation rules, dedup, login success/failure, Google upsert idempotency, getById-missing) + `AuthEndpointsTest` (5, `@SpringBootTest`+MockMvc over Testcontainers Postgres: full signup→login→/api/me flow + 401/409/422 envelopes). Named `*Test` not `*IT` (project uses Surefire, no Failsafe). Full suite: **40 tests, 0 failures** (22 domain + 4 persistence + 9 + 5).

**DEFERRED — 3.3 Google OAuth:** Needs real Google Cloud OAuth2 client ID/secret (user to provide). Endpoints `/api/auth/google/authorize` + `/api/auth/google/callback` and the `upsertGoogleUser` wiring are NOT yet built. `AuthService.upsertGoogleUser` already exists and is tested-ready. Redirect target on success: `{FRONTEND_URL}/auth/complete?token=<JWT>` (§12.1). Resume when creds available. Redirect URI to register: `http://localhost:8080/api/auth/google/callback`.

**Step 3 acceptance status:** password half of §20 Step 3 acceptance MET (signup→login→/api/me via curl). Google-OAuth half PENDING (deferred). Step 3 stays open until 3.3 lands.
