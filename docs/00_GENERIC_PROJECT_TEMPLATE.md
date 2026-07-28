# Generic Project Specification Template

> **Purpose of this document**
> This is a reusable template. Copy it, rename it, fill in the blanks, and paste the completed version as the first prompt to Claude (or any LLM) when you want it to build a full project end-to-end without ambiguity.
>
> The goal is: **every load-bearing decision is made BEFORE code is written**. When you paste this filled-in template to Claude, Claude should be able to execute the entire build with no clarifying questions.
>
> Anywhere you see `<< ... >>` — that's a blank for you to fill in.
> Anywhere you see `[choose: A | B | C]` — pick one and delete the others.
> Delete any sections that don't apply to your project.

---

## 1. PROJECT IDENTITY

- **Project name:** << name >>
- **One-line pitch:** << what the product is, in one sentence a normal person would understand >>
- **Elevator pitch (3–4 sentences):** << the longer version — problem, solution, why it matters >>
- **Target users:** << who uses this and what they get out of it >>
- **Origin story / motivation:** << optional — why you're building it >>

---

## 2. PROBLEM & VALUE

- **Problem being solved:** << the concrete pain point >>
- **Existing alternatives and why they fall short:** << what people do today instead >>
- **Core value proposition:** << the one thing that would make someone switch >>
- **Non-goals (things this product explicitly is NOT):** << list — this is important, keeps scope tight >>

---

## 3. USER TYPES & ROLES

For each user type, describe:

- **Type name:** << e.g. "Creator", "Consumer", "Admin" >>
- **What they can do:**
- **What they cannot do:**
- **How they authenticate:**
- **How their role is assigned:**

---

## 4. CORE USER JOURNEYS

Walk through each primary journey end-to-end. One journey per section.

**Journey N — << name >>**
1. User lands on << page >>
2. They click << button >> and are shown << next screen >>
3. ...
4. Success state: << what completion looks like >>
5. Failure states: << what can go wrong and what the user sees >>

Cover at minimum:
- First-time visitor → signup → first useful action
- Returning user → main productive loop
- Edge cases (empty states, errors, quota exceeded, unauthorized)

---

## 5. FEATURE INVENTORY

Group features into: **Phase 1 (must have to launch)**, **Phase 2 (soon after)**, **Later**.

For every Phase 1 feature:
- **Name:**
- **What it does (user-facing):**
- **What it does (technically):**
- **Dependencies (other features it needs):**
- **Acceptance criteria (how you know it's done):**

---

## 6. TECH STACK (lock exact versions)

- **Backend language + framework:** << e.g. Java 21 + Spring Boot 3.x >>
- **Frontend:** << e.g. React 19 + Vite 6 + Tailwind 3 + shadcn/ui >>
- **Database:** << e.g. PostgreSQL 16 + pgvector >>
- **Cache / rate limit store:** << e.g. Redis 7 >>
- **File / blob storage:** << local disk vs S3-compatible >>
- **AI / LLM providers:** << model + provider + version >>
- **Auth:** << OAuth providers + local password? >>
- **Deployment target:** << Docker Compose local? Cloud? >>
- **Package manager / build tool:** << Maven, npm, etc. >>
- **Rationale for each major choice (1 line each):**

---

## 7. ARCHITECTURE STYLE

- **Overall style:** [choose: layered | hexagonal | modular monolith | microservices | serverless]
- **Strictness:** [choose: strict (zero framework imports in domain) | pragmatic (framework annotations allowed in service layer)]
- **Dependency rule:** << e.g. "arrows point inward, domain has no outside deps" >>
- **Module / package structure:** << full tree >>
- **Cross-cutting concerns (logging, auth, error handling):** << approach for each >>

---

## 8. DOMAIN MODEL

For each core entity:
- **Name:**
- **Fields (name, type, constraints):**
- **Invariants (rules that must always hold):**
- **Relationships to other entities:**
- **Lifecycle (create → states → delete):**

Also define:
- **Value objects** (typed IDs, enums)
- **Aggregate roots** (which entity owns which)

---

## 9. DATABASE SCHEMA

For every table:
- **Table name:**
- **Columns (name, type, constraints, default):**
- **Primary key:**
- **Foreign keys:**
- **Indexes:**
- **Notes (JSONB shape, vector dims, etc.):**

Also:
- **Migration tool:** [Flyway | Liquibase | ...]
- **Naming conventions:** << snake_case tables, singular vs plural, etc. >>
- **Soft delete vs hard delete policy:**

---

## 10. AUTHENTICATION & AUTHORIZATION

- **Providers:** [Google OAuth | GitHub | email+password | magic link | ...]
- **Session strategy:** [JWT | session cookies | ...]
- **Token lifetime + refresh flow:**
- **Password rules (if applicable):**
- **Email verification required?**
- **Role assignment logic (first login and after):**
- **Endpoint-level authorization matrix:** (table of endpoint × role × allowed?)
- **How impersonation / admin override works (if any):**

---

## 11. AI / LLM PIPELINE (if applicable)

**Ingestion (data in):**
- **Supported input types:**
- **Parser per type:** (library + how)
- **Chunking strategy:** (size, overlap, boundaries)
- **Embedding model + dimensions:**
- **Vector storage:** (table shape, index type)
- **Idempotency (re-uploading same file):**
- **Failure handling (partial ingestion):**

**Retrieval (query):**
- **Query embedding model:**
- **Top-K:**
- **Similarity metric:** (cosine, L2, dot)
- **Similarity threshold below which we consider "no relevant results":**
- **Reranking?** [none | cross-encoder | LLM]
- **Filters:** (by user, by folder, by channel, etc.)

**Generation:**
- **LLM model + version:**
- **System prompt template:** (exact text)
- **User prompt template:** (exact text)
- **Tool calling / function calls used:**
- **Streaming?** (yes/no, protocol)
- **Citation format:** (how sources are attached to output)
- **Behavior when retrieval empty:** [refuse | fall back to general model with warning | ask user]

**Cost / usage tracking:**
- **What we log per call:**
- **Per-user quotas:**

---

## 12. API SPECIFICATION

For every endpoint:
- **Method + path:**
- **Auth required?**
- **Request body / query params:**
- **Response body:**
- **Success codes:**
- **Error codes + when returned:**
- **Rate limit:**

Group by domain area (auth / users / files / chats / etc.)

---

## 13. FRONTEND ARCHITECTURE

- **Routing library + route table:**
- **State management approach:** (Zustand, Redux, React Query, plain Context — pick per concern)
- **API client:** (fetch wrapper, axios, tRPC-style, generated client)
- **Streaming client:** (EventSource / fetch-with-reader for SSE)
- **Design system:** (component library, custom, hybrid)
- **Design tokens:** (colors, spacing scale, radii, shadows, font families and sizes)
- **Animation library:** (Framer Motion, GSAP, Lottie, CSS-only)
- **Icon system:** (Lucide, Heroicons, custom SVG)
- **Form handling:** (React Hook Form + Zod?)
- **File upload UI approach:**

---

## 14. PAGE-BY-PAGE SPEC

For every screen in the app:
- **Route:**
- **Purpose:**
- **Who can see it:**
- **Layout description (what's where):**
- **Interactive elements + their behavior:**
- **Empty state:**
- **Loading state:**
- **Error state:**
- **Data it fetches (which endpoints):**
- **Wireframe / visual notes:**

---

## 15. VISUAL DESIGN DIRECTION

- **Overall vibe:** << playful / minimal / brutalist / corporate / etc. >>
- **Reference products / sites for inspiration:**
- **Color palette:** (primary, secondary, accent, neutrals, semantic)
- **Typography:** (heading font, body font, mono font, sizes)
- **Motion philosophy:** << subtle micro-interactions? big theatrical animations? >>
- **Illustration style:** << flat / 3D / hand-drawn / photographic / none >>
- **Component density:** << spacious / compact >>

---

## 16. RATE LIMITING & QUOTAS

- **Where enforced:** (middleware, gateway, service layer)
- **Store:** (Redis, in-memory, DB)
- **Algorithm:** (fixed window, sliding window, token bucket)
- **Limits per endpoint / per user / per plan:**
- **Response when hit:** (status code, retry-after header)

---

## 17. LOGGING, METRICS, ERRORS

- **Log format:** (JSON, plain)
- **Log levels used and when:**
- **What must be logged for every request:** (correlation ID, user, latency, status)
- **Sensitive data redaction rules:**
- **Metrics collected:** (counts, latencies, business events)
- **Error taxonomy:** (validation, auth, not found, conflict, upstream, unknown)
- **Client-facing error shape (JSON):**

---

## 18. LOCAL DEVELOPMENT

- **How to bring up the whole stack in one command:**
- **docker-compose contents (services, volumes, networks):**
- **.env.example (every var, with example value + comment):**
- **Seed data script:**
- **Common dev commands (cheat sheet):**
- **How to run tests:**
- **How to reset local DB:**

---

## 19. TESTING STRATEGY

- **Unit tests:** what layer, what framework, coverage target
- **Integration tests:** what framework (Testcontainers?), what's tested
- **End-to-end tests:** framework, scope
- **Frontend tests:** unit + component + E2E strategy
- **What we explicitly do NOT test in Phase 1:**

---

## 20. BUILD ORDER (execution plan)

Numbered, dependency-ordered steps. For each step:
- **Step N — << title >>**
- **What gets built:**
- **Files created / modified:**
- **Acceptance criteria (how you know it works):**
- **Rough time estimate:**

Ordering rule of thumb:
1. Scaffolding + Docker + DB + a bootable server
2. Auth (nothing else is testable without it)
3. Domain layer + persistence adapters (one entity at a time)
4. Simple CRUD endpoints for each entity
5. Ingestion pipeline
6. Retrieval + LLM integration
7. Streaming
8. Frontend shell + routing + auth
9. Frontend feature pages, one at a time
10. Polish (animations, empty states, error UI)
11. Landing page (or earlier if it's a marketing priority)

---

## 21. OPEN QUESTIONS / UNKNOWNS

List anything you haven't decided yet. If a decision is deferred, note when it must be made and what depends on it.

---

## 22. GLOSSARY

Define every domain term used in this document. This prevents ambiguity — the executor should never have to guess what a word means.

---

## 23. EXECUTION INSTRUCTIONS FOR THE LLM

Paste this at the top when you feed this doc to Claude:

> You are being given a complete build specification for a project.
> Read the entire document before writing any code.
> Follow the architecture, tech stack, and file layout exactly as specified.
> Do not add features not listed in Phase 1.
> Do not swap out dependencies for alternatives without asking.
> Build in the order given in section 20.
> After each build step, stop and show me what you built before moving on.
> If any section is unclear, ask before making assumptions.

---

**End of template.**
