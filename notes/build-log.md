# Build log

Newest entries at the top. One lesson per entry, most important line first.

## Tailwind's default fontWeight scale has no numeric utilities — font-600/700/800 silently compiled to nothing

Verified by grepping a production build's CSS for `.font-600{`: absent, even though the class appeared throughout both the old and new design (Landing.tsx's original `font-800` headings included). Tailwind ships semantic names only (`font-semibold`=600, `font-bold`=700); bare numeric class names aren't arbitrary-value syntax and don't match anything, so they render at the inherited weight with no error, no warning.

Fixed by adding `theme.extend.fontWeight` entries for 400/500/600/700/800 in `tailwind.config.js`. This was caught only because a full re-theme leaned on font-weight for its entire hierarchy (mono captions vs. headings), so a flat-weight page was actually inspected instead of assumed correct. Re-verify with the same grep after any Tailwind config change — a missing utility here fails silent, not loud.

## Re-theming surfaced a second real content bug: half-words from curated phrases leaking into the gap list

"event driven" is a curated multi-word phrase in `KeywordExtractor`, but the unigram pass separately counted "event" and "driven" as their own keywords, so the gap list showed all three: the real phrase plus its two meaningless halves. Same root cause as the first keyword-quality bug (see below) — putting the actual list on screen is what caught it, an assertion on a count wouldn't have. Added "driven", "event", "oriented", "object", "functional" to `NOT_A_SKILL` (leftover halves of already-curated phrases). Test: `doesNotDoubleReportBothAPhraseAndItsLeftoverHalfWord`.

## The keyword extractor was showing users junk, and designing the screen is what exposed it

Putting the gap list on screen as "Add: X" turned a tolerable backend output into an obvious defect. Three separate bugs, all invisible while the terms sat in a JSON blob:

- The token pattern allowed trailing punctuation, so "kafka." and "essential." were reported as skills.
- Stemming chopped "-ing" and "-ed" endings, producing "hir" from "hiring" and "runn" from "running". Plural stripping also broke real names: "kubernetes" became "kubernete" and "redis" became "redi". Verb stemming is gone; `NEVER_STEM` protects technology names that legitimately end in "s".
- Both sides of the comparison were capped at the top 80 terms by frequency. The resume side must be uncapped — a skill mentioned once is still on the page, and capping it reported "add Docker" directly above a resume that listed Docker.

Generic job-posting words now sit in a `NOT_A_SKILL` list and never reach the user, while scoring still counts every token. Lesson worth keeping: render backend output at full size early, because a list a user reads is a much harsher test than an assertion.

## The frontend build was writing compiled .js next to every .tsx source

`tsconfig.json` had no `noEmit`, so `tsc -b` in the build script emitted 31 JavaScript files directly into `src` — `App.js` beside `App.tsx`, and so on. They are stale the moment a source file changes, and a bundler that resolves `.js` before `.tsx` will serve the old one. Set `noEmit: true` and deleted them.

Two follow-ons worth remembering: deleting files under a running Vite server leaves its module graph pointing at them, and the page goes blank with no console error until the dev server is restarted with `node_modules/.vite` cleared. And if these files ever reappear, the build script is the culprit, not the editor.

## With ddl-auto update, a new NOT NULL column needs a SQL default or every query on that table breaks

Adding `plan` to the `users` entity as NOT NULL produced `alter table users add column plan varchar(255) not null`, which Postgres rejects when the table already has rows. Hibernate logs that as a warning and starts anyway, so the column simply does not exist and every read or write of `users` then fails with a 500 — register and login included. The symptom looks nothing like the cause.

Fix: `columnDefinition = "varchar(16) default 'FREE'"` so the ALTER succeeds and existing rows backfill. Apply the same pattern to any future non-null column on a populated table, or switch to real migrations.

## Do not pipe a long-running server through `tail`

`mvn spring-boot:run 2>&1 | tail -80` buffers everything until the process exits, so the captured log stays empty exactly when a running server needs debugging. Run the server bare and read the output file instead. This cost one wasted debugging cycle on the column failure above.

## gpt-4o-mini attributes job-description technologies to the candidate, and prompting alone does not stop it

Verified on a real call: a resume listing Java, Spring Boot, PostgreSQL, Kafka and Docker produced a letter claiming "running services in Docker on AWS" because AWS appeared in the job description. Hardening the system prompt (explicit rule, a worked counter-example, a restated check before each claim) reduced but did not remove it — the second draft still said AWS.

What worked: `UnsupportedTermDetector` scans the generated letter for capitalized terms and acronyms absent from the tailored resume JSON, and `CoverLetterService` re-asks the model once naming the offending terms. Anything still present after that retry is stored on the cover letter row and surfaced in the UI as a claim to check, rather than silently shipped.

Treat this as a property of cheap models generally, not a one-off. When the Anthropic client lands, keep the detector in the path regardless of provider.

## Maven picks JDK 25 locally; the project targets 21

Run Maven with `JAVA_HOME=/opt/homebrew/opt/openjdk@21`. Homebrew's Maven defaults to JDK 25 even though `java` on PATH is 21, and Docker builds on `maven:3.9-eclipse-temurin-21`.

Two failures traced to this, both of which look like code bugs and are not:
- `mvn test` failed with `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN` — Lombok 1.18.34 (the Spring Boot 3.3.4 managed version) cannot do annotation processing on JDK 25. Pinned `lombok.version` to 1.18.46 in the POM so the build survives either JDK.
- Mockito could not create mocks (`Could not modify all classes`) on JDK 25.

## Cover letters travel as JSON, not prose

`OpenAiClient` hardcodes `response_format: {"type":"json_object"}` for every call, so a prose response is not possible through the current `AiClient` interface. The cover letter prompt therefore asks for `{"body": "..."}` and the service unwraps it.

Revisit when the Anthropic client lands: either both providers keep the JSON convention, or the interface grows a response-format argument. Do not let feature code branch on provider to work around this.

## Local verification runs on port 8081

Port 8080 is usually held by the `docker compose` backend container. Start a local build beside it with `-Dspring-boot.run.jvmArguments="-Dserver.port=8081"` and `POSTGRES_HOST=localhost` rather than stopping the container.

## Cover letter service does not depend on GenerationService

It reads `tailoredJson` off the `Generation` entity through Jackson directly. Injecting `GenerationService` created a service-to-service dependency whose only purpose was one JSON parse, and it also forced tests to mock a concrete class.
