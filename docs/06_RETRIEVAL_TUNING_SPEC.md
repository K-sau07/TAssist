# 06 — Retrieval Tuning Spec

**Status:** DRAFT v1 for review · **Branch:** `retrieval-tuning` (off `main` @ e0ac645) · **Type:** Backend RAG tuning · **Scope:** retrieval quality only — no API/DB schema change, no frontend change

> **The discipline for this one:** measure before we tune. We do NOT yank knobs on a hunch. We add read-only diagnostics, run real documents through real questions, look at the actual similarity scores, and only then change values — one at a time, re-measuring after each. Every change stays behind green backend tests.

---

## 1. Problem (observed)

A real question against a channel holding the Gartner "Innovation Insight for Performance Engineering" PDF produced a **grounded but thin** answer: nearly every claim cited the same source marker (`S2`), with only occasional reach to `S6`/`S7`/`S8`. The answer was accurate and honest (grounding works) but drew from a narrow slice of the document.

**Desired outcome (signed off):** *both* broader coverage (pull from more of the document) *and* precise attribution (the right source on each claim).

**Refined goal (Saurabh):** retrieval must surface **all the important, relevant chunks** for a question (don't leave relevant detail behind — the recall problem), while the answer stays **tight to the question asked** and says **exactly what the sources say** — no padding, no drift, no invented framing. Two forces held together:
- **Recall** — get every on-topic chunk in (retrieval levers: floor, topK).
- **Precision + faithfulness** — retrieved chunks are actually on-topic, and generation sticks to them and answers the question directly (generation lever: the prompt that constrains Haiku to the provided context). If Phase B shows answers drifting or padding despite good chunks, the fix is the generation prompt, not retrieval — noted as a possible Phase C(2) lever.

## 2. Current state (measured from code)

Retrieval knobs (`application/retrieval/RetrievalService.java`):
- `SIMILARITY_FLOOR = 0.4` — cosine-similarity cutoff; hits below are dropped.
- `TOPK_SCOPED = 6` — max text chunks for FOLDER/CHANNEL scope.
- `TOPK_MENTIONS = 8` — max when @mention files are targeted.
- `TOPK_SHEETS = 3` — spreadsheet schema hits.

Chunking (`application/ingest/Chunker.java`):
- Target **~500 tokens/chunk, 50 overlap** (chars/4 estimate).
- **PDF: one chunk per page** unless a page > 500 tokens (then split with overlap).
- DOCX packs paragraphs to ~500; PPTX one-per-slide; MD per-heading; TXT greedy-pack.

**Hypothesis (to verify, not assume):** a ~10-page PDF → ~10-14 chunks. With topK=6 and a strict 0.4 floor, only a few pages clear the bar, so the answer leans on 1-2 chunks. The floor and/or topK are the likeliest levers; chunk size is a heavier, later lever.

## 3. The knobs & tradeoffs

| Knob | Raising it | Lowering it | Cost of change |
|---|---|---|---|
| `TOPK_SCOPED` | broader coverage, more citations | sparser, tighter | more tokens → Haiku cost/latency; risk of diluting with weak chunks | 
| `SIMILARITY_FLOOR` | more precise, fewer weak hits | more coverage, lets borderline chunks through | none (config); risk: too low = noise, too high = starved |
| chunk size (ingest) | more context/chunk, fuzzier cites | more precise cites, less context each | **re-embed everything** — heaviest, do last |

## 4. Approach — measure first, then tune

**Phase A — Diagnostics (read-only, no behavior change).**
Add temporary structured logging in `RetrievalService.retrieve()` that records, per query: candidate count, every raw chunk hit with its similarity score (before the floor), how many passed/failed the floor, and the final topK selection. This shows exactly where chunks land relative to 0.4 and whether topK is even being filled. Gated behind a log level / flag so it's noise-free in normal operation.

**Phase B — Measure with a real file (local, reproducible).**
A repeatable local procedure (documented in §6) so Saurabh can verify: upload a known PDF, ask a fixed set of questions, and read the score distribution from the logs. Capture the numbers in this doc before changing anything.

**Phase C — Tune with evidence, one knob at a time.**
Based on Phase B numbers: adjust the single most-indicated knob, re-run the same questions, compare. Repeat. Likely order: (1) floor, (2) topK, (3) chunk size only if precision still poor. Each change is its own commit with the before/after numbers in the message.

**Phase D — Guardrails.**
Add/adjust backend tests so retrieval behavior is pinned (e.g. floor filtering, topK cap, ordering by score). Keep the full suite green.

## 5. Non-negotiables

1. No API/DB schema change; no frontend change. Pure retrieval-quality tuning.
2. Measure before tuning — no blind knob changes; every tune cites its evidence.
3. One knob per commit, re-measured; backend test suite stays green.
4. Chunk-size change (requires re-embedding) is last-resort and called out explicitly if we reach it.
5. All work on `retrieval-tuning`; `main` and `glowup` untouched.

## 6. Local verification procedure (so Saurabh can check)

1. On `retrieval-tuning`, backend running with `.env` (Voyage + Anthropic keys). Docker up.
2. Log in as the channel owner; upload a known text-based PDF to a channel (e.g. the Gartner article). Wait for status READY.
3. In the group room, ask the fixed question set (§7). 
4. Read backend logs for the `retrieve:` diagnostic lines — record candidate count + score distribution per question in §8.
5. After a tune, repeat 3-4 and compare coverage (distinct sources cited) + precision (are cited chunks actually on-topic).

## 7. Fixed question set (for repeatable measurement)

1. "What is the key takeaway of this article?" (broad — should pull many chunks)
2. "What are the risks of performance engineering?" (targeted to one section)
3. "What is the estimated adoption rate?" (single-fact — precision check)
4. "What should software engineering leaders do?" (recommendations — multi-chunk)

## 8. Measurements (filled during Phase B)

*(empty until we run it — numbers go here before any tuning)*

## 9. Acceptance criteria

- Broad questions cite **3+ distinct sources** where the document supports it (coverage).
- Single-fact questions cite the **correct** chunk (precision) and don't pull unrelated ones.
- No regression in the honest-fallback behavior (truly-absent info still returns "no matching source").
- Backend tests green; changes evidence-backed and documented here.

---

*End of draft. No tuning past Phase A diagnostics until this is signed off.*
