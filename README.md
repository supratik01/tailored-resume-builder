# Tailored Resume Builder

AI-powered resume tailoring SaaS. Upload a resume, paste a job description, get back an ATS-optimized tailored resume with a match score, missing-keyword analysis, and PDF/DOCX downloads.

This repo is the **MVP critical path**. See [SCOPE.md](SCOPE.md) for what's in vs. deferred, and [PROMPT.md](PROMPT.md) for the build brief covering the remaining v1 work.

## Status

Working today: JWT auth, PDF/DOCX upload and section parsing, job description keyword extraction, ATS scoring, AI tailoring, PDF/DOCX export of the tailored resume, and cover letter generation with an unsupported-claim check.

Remaining for v1:

- A second `AiClient` implementation (Anthropic) selected by config, alongside the OpenAI one
- Razorpay billing: code is in place (pricing page, checkout call, signed webhook that flips FREE/PRO). Still needs real test keys in `.env` and a publicly reachable webhook URL before a payment can be run end to end.
- A deployed environment

## Stack

- **Frontend**: React 19, TypeScript, Vite, Tailwind, shadcn/ui, TanStack Query, Zustand, React Hook Form, Zod
- **Backend**: Java 21, Spring Boot 3, Spring Security (JWT), JPA/Hibernate, PostgreSQL, Apache PDFBox, Apache POI, OpenHTMLtoPDF
- **AI**: OpenAI Chat Completions, behind the pluggable `AiClient` interface (Anthropic implementation pending — see Status)
- **Billing**: Razorpay (not yet wired up)
- **Infra**: Docker, Docker Compose, Nginx

## Quick start

```bash
cp .env.example .env
# Edit .env — set OPENAI_API_KEY, JWT_SECRET, POSTGRES_PASSWORD
docker compose up --build
```

- Frontend: http://localhost:5186
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Postgres: localhost:5432

## Local dev (no Docker)

Requires Java 21 and Maven 3.9+ on your PATH — no Maven wrapper is committed.

```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend (new shell)
cd frontend
npm install
npm run dev
```

You'll need a local Postgres running on `localhost:5432` with database `resume_builder` and matching user/pass, or set the env vars in `backend/src/main/resources/application.yml`.

## Project layout

```
backend/   Spring Boot service (controller, service, repository, entity, dto,
           config, security, ai, parser, scoring, export, exception packages).
frontend/  Vite + React app (pages, components, lib, store, types).
nginx/     Reverse proxy config for prod compose profile.
```

Design tokens — palette, type scale, radii, shadows, motion — live in [DESIGN.md](DESIGN.md) and as CSS variables in `frontend/src/index.css`. Product positioning and voice are in [PRODUCT.md](PRODUCT.md).

## API surface

| Method | Path                          | Purpose                                     |
|--------|-------------------------------|---------------------------------------------|
| POST   | `/api/auth/register`          | Create account                              |
| POST   | `/api/auth/login`             | Login, returns access + refresh token       |
| POST   | `/api/auth/refresh`           | Exchange refresh token                      |
| POST   | `/api/resumes/upload`         | Upload PDF/DOCX, returns parsed resume      |
| GET    | `/api/resumes`                | List user's resumes                         |
| GET    | `/api/resumes/{id}`           | Fetch one parsed resume                     |
| POST   | `/api/generations`            | Tailor a resume against a JD                |
| GET    | `/api/generations`            | List past generations                       |
| GET    | `/api/generations/{id}`       | Fetch a generation incl. ATS score          |
| POST   | `/api/generations/{id}/cover-letter` | Write (or rewrite) the cover letter  |
| GET    | `/api/generations/{id}/cover-letter` | Fetch the stored cover letter        |
| GET    | `/api/billing/usage`          | Plan, runs used this month, runs left        |
| POST   | `/api/billing/checkout`       | Create a Razorpay subscription for Checkout  |
| POST   | `/api/billing/webhook`        | Razorpay events; HMAC-signed, no JWT         |
| GET    | `/api/generations/{id}/pdf`   | Download tailored resume as PDF             |
| GET    | `/api/generations/{id}/docx`  | Download tailored resume as DOCX            |

## Env vars

See [.env.example](.env.example). Required: `OPENAI_API_KEY`, `JWT_SECRET` (256-bit hex), `POSTGRES_PASSWORD`.

## Tests

```bash
cd backend && mvn test
```

Backend covers the parser, ATS scoring, and the auth flow. The frontend has Vitest configured but no test files yet, so `npm test` currently runs nothing.
