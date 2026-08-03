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
| D5 | Embedding provider | **UNRESOLVED — decide before Step 5** | Spec §21 open Q#1: Voyage `voyage-3` (1024-dim) vs OpenAI `text-embedding-3-small` (1536-dim). Load-bearing: sets `VECTOR(n)` in migrations V4/V5 and cannot change after data exists. Spec default assumption = Voyage 1024. |
| D6 | Spring AI Anthropic artifact | `spring-ai-starter-model-anthropic` on BOM `1.0.9` | Spec §20 named the pre-GA id `spring-ai-anthropic-spring-boot-starter`, which does not exist in Central. Renamed at Spring AI 1.0.0 GA. |
| D7 | Bucket4j coordinates | `com.bucket4j:bucket4j_jdk17-core:8.19.0` | Spec §6 implies Bucket4j but pins no version. `8.10.1` only exists under the legacy `bucket4j-core` id; the maintained JDK17+ line (`_jdk17-core`) starts at `8.19.0`. Appropriate for Java 21. |
| D8 | Frontend build tool versions | Vite `6.4.3`, TypeScript `5.9.3`, ESLint (not oxlint) | `create-vite@9` scaffolds Vite 8 / TS 6 / oxlint. Pinned back to Vite 6 / TS 5 per §6 (spec locks Vite 6). Kept ESLint because §23 references `eslint-plugin-jsx-a11y`. |
| D9 | shadcn/ui timing | Deferred to Step 15 | shadcn is a CLI that copies component source on demand, not a runtime npm dep. Radix/`clsx`/`tailwind-merge`/`cva` helpers installed now; `shadcn init` runs in Step 15 when components are themed and used. |
| D10 | DC create_file unreliable | Use bash heredoc + verify | This session, Desktop Commander create_file/str_replace reported success but silently dropped writes; start_process hung repeatedly. Write files via `cat > f <<'EOF'` and confirm with `wc -l`. |

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
