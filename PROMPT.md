# Master Prompt: AI Resume Tailoring Platform — MVP Completion Pipeline

(Claude Fable 5 / Mythos 5 — autonomous coding agent)

## 0. Context — why this exists

I'm building Bytefront's AI Resume Tailoring Platform, a SaaS for job seekers who currently burn hours manually rewriting their resume for every application. The paying user is an individual job seeker at ₹499 per month; recruiting teams are a later, separate audience. A working codebase already exists in this repository — your job is to finish the remaining MVP surface and get it to a deployed, demoable state without me babysitting each step. Read this brief in full before writing code.

## 1. Where the build actually is

Already built and in the repo (treat as the foundation, not as work to redo):

- JWT auth: register, login, refresh, BCrypt hashing (`AuthController`, `AuthService`, `JwtService`).
- Resume upload and parsing for PDF and DOCX (`TextExtractor`, `ResumeStructureExtractor`, `ResumeService`), stored as a `@Lob` on the `Resume` entity in Postgres.
- Job description input with keyword extraction (`JobDescription`, `KeywordExtractor`).
- ATS scoring: keyword match percentage, missing keywords, suggestions (`AtsScoringService`, `AtsScore`).
- AI tailoring behind a pluggable interface (`AiClient` with `complete(systemPrompt, userPrompt)`, one OpenAI implementation, prompts in `PromptBuilder`).
- Export of the tailored resume to PDF and DOCX (`PdfExporter`, `DocxExporter`, `ResumeHtmlRenderer`).
- React frontend: landing, login, register, dashboard, upload, tailor, results — routed in `frontend/src/App.tsx`.
- Docker Compose for the whole stack, Nginx reverse proxy, Swagger at `/swagger-ui.html`, backend unit and integration tests.

Remaining for v1 — this is what "done" means, and it is the whole job:

1. **Cover letter generator.** One-page letter grounded in the tailored resume and the job description, going through `AiClient` like every other AI feature. Never invent experience, employers, titles, or dates.
2. **Second AI provider behind the existing interface.** Add an Anthropic implementation of `AiClient` alongside `OpenAiClient`, selected by configuration in `AppProperties`, so a cheaper model can serve simple work and a stronger one can serve tailoring and cover-letter writing without any feature code changing. One interface, two implementations, config-selected. Do not build a routing or fallback framework.
3. **Free-tier quota.** A monthly run limit enforced server-side. The limit must be a configuration value under `AppProperties` (something like `app.quota.free-runs-per-month`), not a constant in service code, so I can tune it after seeing real API cost.
4. **Razorpay subscription billing.** ₹499 per month, standard choice for INR SaaS. Checkout on the frontend, subscription state and webhook handling on the backend, quota enforcement reading from that state.
5. **Deployment.** A URL I can open and use end to end.

Explicitly deferred — do not build these. Leave the navigation or route stub only where it costs nothing:

- Portfolio analyzer (needs its own ingestion path for links, GitHub, case studies).
- Interview question generator.
- Recruiter mode (different user type, permissions model, and pricing — a v2 product surface, not a toggle).
- Google OAuth, admin dashboard and analytics, async or queued generation, multilingual resumes.

If you find yourself building any deferred item before the five items above are demoable end to end, stop — that is scope creep, not thoroughness.

## 2. Stack and hard constraints

The stack is settled. Work inside it.

- **Frontend:** React 19, TypeScript, Vite, Tailwind, shadcn/ui components under `frontend/src/components/ui`, TanStack Query, Zustand, React Hook Form, Zod.
- **Backend:** Java 21, Spring Boot 3, Spring Security with JWT, JPA and Hibernate.
- **Database:** PostgreSQL.
- **AI:** every call goes through `AiClient`. No feature calls a vendor SDK directly.
- **File handling:** resume content lives in Postgres as it does today. Do not introduce S3, Cloudflare R2, or any other object store for v1.
- **Infra:** Docker, Docker Compose, Nginx.

What is fixed is the stack above and the visual language. Everything else is yours to change where a v1 item genuinely calls for it.

Fixed:

- **The stack.** Don't swap frameworks, don't move the backend to another language or runtime, don't replace Postgres, and don't route around `AiClient` to call a vendor SDK from feature code. Don't add a library or hosted service beyond the list above without a concrete reason tied to one of the five remaining v1 items.
- **The theme.** `DESIGN.md` defines the palette, typography, radii, shadows, and motion, and those tokens live as CSS variables in `frontend/src/index.css`. Build from those variables and the shadcn/ui components in `frontend/src/components/ui`. No new color system, no new font family, no gradient-heavy "AI SaaS" styling — `PRODUCT.md` lists that explicitly as an anti-reference. Add a token if a new surface truly needs one; don't start a second design language.

Open to change:

- **Pages and components.** Rework existing screens, add routes, restructure the navigation, split or replace components. The current pages are a starting point, not a contract. If billing and quota mean the dashboard and results pages need reshaping, reshape them.
- **Backend packages.** Add packages, move classes, rename where it makes the code clearer. Keep it recognizably layered rather than reorganizing for its own sake.
- **The database.** Add entities, add columns, and change existing ones where a feature needs it. Prefer additive changes because they're cheaper to reason about, but a real schema change is fine — there is no production data yet. Migrating or resetting the local development database is not something you need to ask about.

## 3. Effort and pacing

Default to high effort for architecture decisions, for the prompt design behind the cover letter feature, and for anything touching money or auth. Drop to medium for boilerplate: CRUD endpoints, DTOs, form scaffolding, routine React components.

Don't add features, refactor, or introduce abstractions beyond what these five items require. A cover letter generator doesn't need a template plugin system. Don't design for hypothetical future requirements — do the simplest thing that works well. Don't add error handling or fallbacks for situations that can't occur at MVP scale. Validate at real boundaries only: file upload, job description text, Razorpay webhooks, and anything crossing the auth line.

When you have enough information to act, act. The scope split above is decided; don't re-litigate it. If you're weighing an implementation choice, give me a recommendation and move, not an options survey.

## 4. Boundaries — what needs my sign-off

Proceed without asking on anything reversible that follows from this brief: writing code, adding endpoints, writing prompts, wiring routes, running the stack locally, running tests.

Stop and ask me before:

- Spending real money — AI API usage at scale, paid hosting beyond free-tier development use, or going live with production Razorpay keys.
- Any action touching a real user's data. There is none yet; flag it before that changes.
- Deciding the ATS scoring rubric's weights, if you are changing them and are not confident they reflect real applicant tracking system behavior. Tell me your assumption and let me correct it rather than guessing silently into the scoring logic.
- Anything irreversible outside the development environment: force-pushing, rotating keys, or touching a deployed database. Local schema changes and development-database resets don't need sign-off.

If you hit one of these, ask and end the turn there rather than ending on a promise to ask later. Everything else: build it and show me.

## 5. Grounding progress claims

Before reporting progress, check each claim against an actual tool result from this session — a passing test, a successful build, a real API response. Report outcomes faithfully. If the cover letter endpoint returns a 500 on a test run, say so with the actual error rather than calling it mostly working. If Razorpay sandbox checkout has not been exercised end to end, say that plainly instead of implying it has.

## 6. Self-verification

After each of the five remaining pieces is implemented — cover letter, second provider, quota, billing, deployment — re-read this brief's description of that piece with fresh eyes and confirm the implementation does that specific thing rather than something adjacent. Run the backend tests and a frontend build as part of that check. Don't wait until all five are done to discover one drifted.

## 7. Build log across sessions

Keep a running log at `notes/build-log.md`. One entry per lesson, most important line first: a decision and why, a bug and its real root cause, an approach that didn't work. Don't log what the code already shows. Update an existing entry rather than duplicating it, and correct an entry in place when you learn it was wrong.

## 8. How to report back to me

Terse shorthand between steps is fine — that's you working. The final summary is different: it's for someone who didn't watch any of it.

Open with the outcome in one sentence — what works, what doesn't — then the supporting detail. Write full sentences and spell things out. Give each file, route, or environment variable its own plain clause instead of stacking identifiers together. If you hit a decision point from section 4, ask there and stop.

## 9. Definition of done for v1

A job seeker can sign up, upload a resume, paste a job description, get an ATS score with a gap list, get a tailored resume, download it as PDF or DOCX, get a matching cover letter, hit the free-quota paywall, pay ₹499 per month through Razorpay sandbox, and keep working. All of it running at a URL I can click and try myself. Nothing on the deferred list blocks this.
