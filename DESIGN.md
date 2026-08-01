# Design System — Tailr

Code-review / dev-tool aesthetic. Register: product (design serves the tool); the
landing page borrows the same system in a marketing register. Full brief lived in the
chat that specified this system — this file is the maintained reference going forward.

## Point of view

Resumes, job-description parsing, ATS keyword matching, and the moment a match score
improves — grounded in a git-diff / code-review visual language. Rewrites read as a
diff (`−` old line, `+` new line). The score reads like an instrument. The canvas is a
faint blueprint grid.

Deliberately avoided: cream + terracotta, near-black + neon-green, hairline-rule
newspaper layout, gradient text, side-stripe cards.

## Color (OKLCH, cool neutrals tinted toward hue 262 — indigo)

```
--canvas:        oklch(0.955 0.008 260)   /* backdrop behind panels, carries the grid */
--grid:          oklch(0.9 0.012 262 / 0.55)
--surface:       oklch(0.995 0.002 260)   /* panel / screen background, never #fff */
--surface-sunk:  oklch(0.975 0.006 260)   /* preview cards, telemetry columns */

--ink:           oklch(0.26 0.02 262)     /* primary text, never #000 */
--ink-soft:      oklch(0.45 0.02 262)     /* body, secondary */
--ink-faint:     oklch(0.56 0.02 262)     /* mono captions, // comments */
--ink-inverse:   oklch(0.97 0.004 260)    /* text on dark panels */

--hairline:        oklch(0.91 0.01 262)
--hairline-strong: oklch(0.85 0.014 262)

--accent:         oklch(0.54 0.16 262)    /* brand indigo — primary buttons, active state */
--accent-hover:   oklch(0.47 0.16 262)
--accent-wash:    oklch(0.96 0.03 262)
--accent-on-dark: oklch(0.72 0.13 262)

--dark-panel:    oklch(0.16 0.02 262)     /* diff block, paywall plan card */
--dark-panel-2:  oklch(0.21 0.022 262)

/* Signal colors — meaning only, never decoration */
--good:        oklch(0.62 0.13 155)   --good-wash:   oklch(0.95 0.05 155)
--gap:         oklch(0.72 0.13 70)    --gap-wash:    oklch(0.95 0.06 78)
--remove:      oklch(0.6 0.15 25)     --remove-wash: oklch(0.95 0.04 25)
```

Strategy: **Restrained** — tinted neutrals + one accent, signal colors carrying the
score/gap/diff emotional beats. Dark panels are purposeful (diff block, score band,
paywall card), not decorative.

## Typography

- **IBM Plex Sans** — UI, headings, body. Weights 400/500/600/700.
- **IBM Plex Mono** — data, scores, keywords, diffs, wordmark, `//` captions, chips,
  severity tags. This is where the dev-tool voice lives.
- Hierarchy from scale + weight, not color. Numeric weight utilities (`font-600`,
  `font-700`) are wired into `tailwind.config.js` under `theme.extend.fontWeight` —
  Tailwind's default scale only ships semantic names (`font-semibold`), so this
  extension is required or the classes compile to nothing. Verify after any Tailwind
  config change by grepping a build's CSS for `.font-600{`.

## Layout & components

- **Blueprint canvas**: `.blueprint` / `.blueprint-dark` utility classes (defined in
  `index.css`) — a 34px grid, light or dark. Used behind every full-screen surface.
- **Wordmark**: `Wordmark.tsx` — `›` caret in a rounded accent square + lowercase mono
  "tailr".
- **Top bar**: wordmark + `FlowStepper` (`1 Setup › 2 Score › 3 Tailor`, mono) + usage
  chip + email + log out.
- **Buttons**: `AppButton.tsx` — the only button component. Variants: primary (accent
  fill), secondary (surface + hairline), ghost, onDark. Every label states the action
  ("Score my resume →", "Download resume (.docx) ↓").
- **Score ring**: `ScoreRing.tsx` — SVG, rounded cap, `rotate(-90)`, prop-driven
  `strokeDasharray`. `scoreColor` / `scoreVerdict` exported so every surface reads a
  score identically: ≥75 good, ≥55 gap, below error.
- **Chips**: `Chip.tsx` — mono, wash background + matching border, `✓ term` (found) /
  `× term` (gap).
- **Diff panels**: `ResumeDiff.tsx` — dark blueprint panel, mono, `−` red wash / `+`
  green wash per line. Built from `lib/diffResume.ts`, which diffs the real source
  resume against the tailored one (index-aligned by role, then by bullet) — never a
  fabricated or cosmetic diff.
- **Fix queue**: `GapList.tsx` — bordered rows (`divide-y`), not cards. Each row: `ADD`
  tag, the term, occurrence count in the posting, and a point value pulled from
  `KeywordGap.pointsIfAdded` (backend-computed from the real scoring weights — never a
  client-side guess).
- **Rows over cards** everywhere a list is the content, per the shared design laws.

### Motion

Hover states only (~150ms). No layout animation, no bounce. `ScoreRing` and
`AtsScoreBoard` bars animate in once on mount (the one exception — a value revealing
itself is not decoration).

## Copy voice

Plain, active, job-seeker language, never internal system terms — "your resume" and
"this job," not "payload" or "parser output". Gaps are specific and actionable ("Add
kubernetes — appears 2× in the post") with a real point value. No em dashes in UI copy
where avoidable (mono diff lines use `−`/`+`, not em dash). The paywall is a soft next
step, framed around value already received, not a wall.

## Honesty constraints baked into the data model

Two backend fields exist specifically so the UI never has to invent a number:

- `KeywordGap.pointsIfAdded` — derived from the real keyword-match scoring weight
  (`AtsScoringService.KEYWORD_WEIGHT`), not eyeballed.
- `GenerationResponse.baselineScore` — the source resume scored against the same
  posting, before tailoring, using the identical formula. Powers the "56 → 78" delta in
  the results header. Null for generations scored before this field existed; the UI
  hides the delta rather than backfilling a guess.

## Known gaps (flagged, not fixed, during the last design pass)

- `ResumeStructureExtractor` mis-parses some plain-text-derived `.docx` files: the
  company/date line gets absorbed into the experience title and zero bullets are
  extracted. When this happens, `ResumeDiff` correctly shows every tailored bullet as a
  pure addition (there is nothing to remove) — that is the diff behaving honestly given
  bad input, not a diff bug. Worth a parser fix separately.
