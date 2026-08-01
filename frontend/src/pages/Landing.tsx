import { motion, useReducedMotion } from "framer-motion";
import { ScoreRing } from "@/components/ScoreRing";
import { AppButton } from "@/components/AppButton";
import { Chip } from "@/components/Chip";

const HOW = [
  { n: "01", title: "Upload your resume", body: "PDF or DOCX. Sections come out structured: experience, skills, education, projects." },
  { n: "02", title: "Paste the job post", body: "The whole posting. It reads it the way an applicant tracking system does." },
  { n: "03", title: "Get the diff", body: "Rewritten bullets, a match score, a cover letter. Every change traceable to why it happened." },
];

export function Landing() {
  const reduce = useReducedMotion();
  // One reveal variant for the page. Under reduced motion it renders in place.
  const fade = {
    initial: reduce ? false : { opacity: 0, y: 8 },
    whileInView: { opacity: 1, y: 0 },
    viewport: { once: true },
  } as const;

  return (
    <div style={{ background: "var(--surface)" }}>
      {/* ── Hero, on the blueprint canvas ─────────────────────── */}
      <section className="blueprint relative overflow-hidden">
        <div className="container relative grid items-center gap-14 py-24 md:py-28 lg:grid-cols-[1.05fr_0.95fr] lg:gap-10 lg:py-32">
          <motion.div {...fade} transition={{ duration: 0.5 }}>
            <p className="font-mono-plex mb-5 text-xs" style={{ color: "var(--ink-faint)" }}>
              // stop rewriting your resume for every posting
            </p>
            <h1
              className="font-display text-5xl font-700 leading-[1.08] tracking-tighter text-balance md:text-6xl"
              style={{ color: "var(--ink)" }}
            >
              Your resume,
              <br />
              <span style={{ color: "var(--accent)" }}>tailored per job.</span>
            </h1>

            <p className="mt-6 max-w-lg text-pretty text-base leading-relaxed" style={{ color: "var(--ink-soft)" }}>
              Upload your resume once. Paste the job description. Tailr scores the match,
              rewrites your bullets against it, and shows you exactly what changed and why.
              Nothing invented, just reframed.
            </p>

            <div className="mt-9 flex flex-wrap items-center gap-3">
              <AppButton to="/register" size="lg">
                Score my resume →
              </AppButton>
              <AppButton to="/login" variant="secondary" size="lg">
                Log in
              </AppButton>
            </div>

            <p className="font-mono-plex mt-5 text-xs" style={{ color: "var(--ink-faint)" }}>
              3 free tailors/mo · no card required
            </p>
          </motion.div>

          {/* Score + diff proof panel */}
          <motion.div {...fade} transition={{ duration: 0.5, delay: 0.15 }} className="max-w-md lg:justify-self-end">
            <div
              className="rounded-2xl p-5"
              style={{ background: "var(--surface)", border: "1px solid var(--hairline)", boxShadow: "var(--shadow-lg)" }}
            >
              <div className="flex items-center gap-5">
                <ScoreRing score={74} size={84} strokeWidth={9} />
                <div className="min-w-0">
                  <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
                    senior-backend-engineer.txt
                  </p>
                  <p className="mt-1 font-600" style={{ color: "var(--ink)" }}>
                    Close, with gaps
                  </p>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    <Chip term="kubernetes" state="gap" />
                    <Chip term="terraform" state="gap" />
                  </div>
                </div>
              </div>
            </div>

            {/* Git-style diff, revealed just after the score so it reads as the proof behind it */}
            <motion.div
              initial={reduce ? false : { opacity: 0, y: 10 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: 0.35, ease: [0.16, 1, 0.3, 1] }}
              className="blueprint-dark mt-4 overflow-hidden rounded-2xl"
              style={{ border: "1px solid oklch(1 0 0 / 0.08)" }}
            >
              <div className="flex items-center gap-2 border-b px-4 py-2.5" style={{ borderColor: "oklch(1 0 0 / 0.08)" }}>
                <span className="font-mono-plex text-2xs" style={{ color: "oklch(0.6 0.02 262)" }}>
                  experience/senior-engineer.diff
                </span>
              </div>
              <div className="font-mono-plex px-4 py-3.5 text-[12.5px] leading-relaxed">
                <p style={{ background: "var(--remove-wash)", color: "oklch(0.55 0.13 25)" }} className="rounded px-2 py-1">
                  − Worked on frontend features and helped ship product updates.
                </p>
                <p className="mt-1.5 rounded px-2 py-1" style={{ background: "var(--good-wash)", color: "oklch(0.4 0.1 155)" }}>
                  + Shipped 14 React features across 3 quarters, reducing time-to-interactive by{" "}
                  <mark style={{ background: "transparent", color: "var(--accent-on-dark)", fontWeight: 600 }}>31%</mark>
                  {" "}and lifting mobile conversion 18%.
                </p>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* ── How it works, connected columns, not cards ───────── */}
      <section style={{ background: "var(--surface)" }} className="py-24 md:py-32">
        <div className="container">
          <h2 className="font-display text-3xl font-700 tracking-tighter text-balance md:text-4xl" style={{ color: "var(--ink)" }}>
            Three steps. Every one traceable.
          </h2>

          <div className="mt-16 grid gap-0 md:grid-cols-3">
            {HOW.map((s, i) => (
              <div key={s.n} className="relative py-8 pr-8 md:pr-12">
                {i > 0 && <div className="absolute left-0 top-0 hidden h-full w-px md:block" style={{ background: "var(--hairline)" }} />}
                {i > 0 && <div className="mb-8 h-px md:hidden" style={{ background: "var(--hairline)" }} />}
                <div className="pl-0 md:pl-8">
                  <span className="font-mono-plex text-3xl font-600" style={{ color: "var(--hairline-strong)" }}>
                    {s.n}
                  </span>
                  <h3 className="mt-4 text-lg font-600 tracking-snug" style={{ color: "var(--ink)" }}>
                    {s.title}
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed" style={{ color: "var(--ink-soft)" }}>
                    {s.body}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Pricing strip ──────────────────────────────────────── */}
      <section className="py-14">
        <div
          className="container flex flex-col items-start justify-between gap-4 border-y py-8 sm:flex-row sm:items-center"
          style={{ borderColor: "var(--hairline)" }}
        >
          <p className="text-sm" style={{ color: "var(--ink-soft)" }}>
            <span className="font-600" style={{ color: "var(--ink)" }}>
              Free: 3 tailors a month.
            </span>{" "}
            One paid plan: <span className="font-mono-plex">₹499/mo</span>, unlimited, cancel anytime.
          </p>
          <AppButton to="/pricing" variant="secondary" size="sm">
            See pricing →
          </AppButton>
        </div>
      </section>

      {/* ── Credibility footer ─────────────────────────────────── */}
      <section className="blueprint-dark py-20">
        <div className="container text-center">
          <h2 className="font-display text-3xl font-700 tracking-tighter text-balance md:text-4xl" style={{ color: "var(--ink-inverse)" }}>
            Built by an engineer who was tired of rewriting his own resume.
          </h2>
          <p className="mx-auto mt-4 max-w-md text-pretty text-sm" style={{ color: "oklch(0.68 0.02 262)" }}>
            No fabricated experience, no keyword stuffing. Just your resume, read closely
            against the job, and rewritten where it's honestly true.
          </p>
          <div className="mt-8">
            <AppButton to="/register" variant="onDark" size="lg">
              Score my resume →
            </AppButton>
          </div>
        </div>
      </section>
    </div>
  );
}
