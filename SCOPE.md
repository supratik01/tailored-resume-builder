# Scope

## In this round — built

- JWT auth: register, login, refresh, password hashing (BCrypt)
- Resume upload (PDF/DOCX) with text extraction and section parsing
- Job description input with keyword extraction
- AI tailoring engine behind pluggable `AiClient` (OpenAI implementation included)
- ATS scoring: keyword match %, missing keywords, suggestions
- Resume export: PDF (OpenHTMLtoPDF) and DOCX (Apache POI)
- React frontend: landing, login/register, dashboard, upload, tailoring, results
- Docker Compose for the whole stack
- OpenAPI/Swagger docs
- Backend unit + integration tests for the critical services
- Cover letter generation with tone selection and a claim check that flags anything the resume does not evidence
- Free-tier quota: configurable monthly tailoring runs, enforced server-side, with a FREE/PRO plan flag on the user

## In this round — remaining

See [PROMPT.md](PROMPT.md) for the full brief on these.

- **Second `AiClient` implementation** (Anthropic) beside `OpenAiClient`, selected by config in `AppProperties` — cheaper model for scoring, stronger for tailoring and cover letters, with no feature code touching a vendor SDK.
- **Razorpay subscription billing** at ₹499/month — checkout on the frontend, subscription state and webhook handling in a new backend billing package, quota reading from that state.
- **Deployment** to a URL that runs the flow end to end.

The tech stack is fixed — Spring Boot and JPA on Postgres, React with Vite, Tailwind, and shadcn/ui, all AI calls through `AiClient` — as is the design language in [DESIGN.md](DESIGN.md). Pages, packages, and schema are open: rework screens, add or change entities, and reorganize where a feature calls for it. There's no production data, so migrations are cheap.

## Deferred (call out, not built)

- Watermarked free-tier PDFs
- Google OAuth (email/password JWT only this round)
- Admin dashboard + analytics
- Portfolio analyzer, LinkedIn optimizer, interview question generator, keyword heatmap
- Recruiter mode (separate user type, permissions model, and pricing — a v2 surface)
- Async/queue processing (generation is synchronous for MVP)
- CDN/edge caching, multilingual resumes

Each deferred area has a clear extension point — start from `backend/src/main/java/com/tailored/resume/ai/AiClient.java`.
