# TAssist

A TA-managed, course-scoped AI doubt platform. Teaching Assistants upload lecture
slides for their courses. Students log in, pick a course, and ask questions.
Answers are grounded in course material via RAG and streamed back in real time.

Built because students ask the same questions repeatedly and TAs answer them manually,
often outside office hours. Course material already exists — lecture slides, notes,
assignment specs. TAssist puts it to work as a first-responder for student doubts,
available 24/7.

---

## How it works

**TA side**
- Creates a course and uploads lecture PPTs
- System extracts text slide by slide, chunks it, embeds it, stores in pgvector
- Dashboard shows what questions students are asking

**Student side**
- Logs in, picks a course from the list
- Asks a question — gets a streamed answer grounded in the actual lecture material
- Each answer cites which lecture and slide the context came from
- Full chat history per course

---

## Stack

- Java 21, Spring Boot 3, Virtual Threads
- Spring AI — embeddings and RAG pipeline
- Anthropic Claude Haiku — response generation
- PostgreSQL + pgvector — vector similarity search
- Redis — rate limiting
- Apache POI — PPT text extraction
- React 19 + Vite 6 + Tailwind 3
- Docker Compose

---

## Architecture

Hexagonal (Ports & Adapters). Thin controllers, fat services, typed records throughout.
Dependency arrows point inward. Domain layer has zero framework imports.

```
domain/         models, use case interfaces, output ports
application/    services implementing use cases
infrastructure/ persistence, AI adapters, PPT extraction, web layer
```

---

## Running locally

```bash
# start postgres (pgvector) and redis
docker compose up -d

# backend
cp .env.example .env   # fill in GOOGLE_CLIENT_ID, ANTHROPIC_API_KEY, JWT_SECRET
cd backend && mvn spring-boot:run

# frontend
cd frontend && npm install && npm run dev
```

---

## Roles

**TA** — authenticated via Google OAuth. Creates courses, uploads PPTs, views analytics.

**Student** — authenticated via Google OAuth. Lists courses, starts chat sessions, asks questions.

Role is assigned on first login based on email or a seeded allow-list in the database.

---

## Status

Active development. Phase 1 in progress — auth, course management, ingestion pipeline, SSE streaming.
