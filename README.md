# TAssist

A personal AI assistant that answers questions **grounded in your own uploaded documents** —
never invented, always cited. Optionally publish a curated, private **Q&A channel** over a
subset of your files so approved members can ask questions without ever seeing the files.

Two products in one account:

- **Private library** — an Obsidian-style document vault with an LLM on top. Upload PDFs,
  Word docs, Excel sheets, slides, and notes, then chat with them by name using `@filename`
  mentions or scope a chat to a folder.
- **Channels** — create a channel with a public `@username`, attach some of your files, and
  invite people to ask questions. They get answers grounded in your docs, with owner-set
  display labels instead of real filenames.

Every answer is a paraphrased, cited quote from the underlying documents. If the answer isn't
in your files, TAssist says so instead of guessing (fallback mode, clearly labeled).

> **Core invariant:** raw files never reach the LLM — only retrieved chunk text does (RAG throughout).

---

## Stack

**Backend** — Java 21, Spring Boot 3.3.5, hexagonal architecture (domain -> application -> infrastructure).
Postgres + pgvector, Redis, Flyway migrations. Voyage AI embeddings (`voyage-3.5`, 1024-dim),
Claude (`claude-haiku-4-5`) for generation. SSE streaming, server-side tool-use for spreadsheets.

**Frontend** — React 19, Vite 6, TypeScript, Tailwind 3, TanStack Query, Zustand,
React Hook Form + Zod, Framer Motion, lucide-react.

---

## Getting started

### Prerequisites

- JDK 21 (Temurin recommended)
- Node 18+ and npm
- Docker + Docker Compose (for Postgres + Redis)
- API keys: a Voyage AI key (embeddings) and an Anthropic key (generation)

### 1. Start infrastructure

```bash
docker compose up -d          # starts tassist-postgres (pgvector) + tassist-redis
docker compose ps             # both should be "healthy"
```

### 2. Configure secrets

Create a `.env` file in the repo root (git-ignored):

```bash
VOYAGE_API_KEY=your_voyage_key
ANTHROPIC_API_KEY=your_anthropic_key
# Google OAuth (optional — email/password works without these)
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

> **Heads up:** Spring Boot does not read `.env` natively. You must source it before running
> (see below). If `VOYAGE_API_KEY` is missing, uploads will fail at the embedding step with a
> `FAILED` status and the reason "VOYAGE_API_KEY not configured".

### 3. Run the backend

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home   # macOS/Temurin
set -a; . ../.env; set +a          # source secrets into the environment
mvn spring-boot:run                # serves on http://localhost:8080
```

Health check: `curl http://localhost:8080/api/health` -> `{"ok":true}`

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev                        # serves on http://localhost:5173, proxies /api -> :8080
```

Open http://localhost:5173, sign up, and upload your first file.

---

## Development

**Backend**

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn test           # full suite (Testcontainers spins up throwaway Postgres for integration tests)
mvn compile        # compile only
```

**Frontend**

```bash
cd frontend
npm run typecheck  # tsc --noEmit
npm run lint       # eslint + jsx-a11y
npm run build      # tsc -b && vite build
```

---

## Project layout

```
backend/          Spring Boot app (domain / application / infrastructure)
  src/main/resources/db/migration/   Flyway V1..V8
frontend/         React + Vite app
  src/lib/        api clients, sse streaming client, auth store, query hooks
  src/features/   landing, auth, dashboard, files, folders, chat, channels, widgets
  src/design/     design tokens (theme.css) + primitives
docs/
  01_TASSIST_SPEC.md   the immutable spec (source of truth)
  BUILD_LOG.md         per-step build log with dated DONE & VERIFIED entries
```

---

## How it works (RAG pipeline)

1. **Ingestion** — an uploaded file is parsed (PDFBox / Apache POI / commonmark), split into
   ~500-token chunks with 50-token overlap, embedded with Voyage, and stored in pgvector.
   Spreadsheets use structured mode: a schema summary is embedded and rows are queried via a
   safe parameterized `query_spreadsheet` tool call.
2. **Retrieval** — a question is embedded and matched against the user's (or channel's) chunks
   by cosine similarity above a threshold.
3. **Generation** — Claude answers strictly from the retrieved excerpts, emitting `[S{n}]`
   citations. Answers stream token-by-token over SSE. If nothing clears the threshold, it falls
   back to general knowledge with a mandatory disclaimer.

---

## Status

MVP complete (spec build steps 0-23). See `docs/BUILD_LOG.md` for the full history.
Phase 2 ideas are tracked in the spec's "Open questions" section.
