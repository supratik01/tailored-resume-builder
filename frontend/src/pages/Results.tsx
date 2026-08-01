import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { Download, FileText, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import { GenerationResponse, ResumeResponse } from "@/types";
import { AtsScoreBoard } from "@/components/AtsScoreBoard";
import { ResumePreview } from "@/components/ResumePreview";
import { CoverLetterPanel } from "@/components/CoverLetterPanel";
import { GapList } from "@/components/GapList";
import { ResumeDiff } from "@/components/ResumeDiff";
import { Chip } from "@/components/Chip";
import { diffResume } from "@/lib/diffResume";
import { ScoreRing, scoreVerdict } from "@/components/ScoreRing";
import { FlowStepper } from "@/components/FlowStepper";
import { AppButton } from "@/components/AppButton";
import { useAuthStore } from "@/store/auth";

type View = "diff" | "resume" | "letter";

export function Results() {
  const { generationId } = useParams<{ generationId: string }>();
  const token = useAuthStore((s) => s.accessToken);
  const [view, setView] = useState<View>("diff");

  const generation = useQuery({
    queryKey: ["generation", generationId],
    queryFn: () => api.get<GenerationResponse>(`/api/generations/${generationId}`).then((r) => r.data),
    enabled: !!generationId,
  });

  const source = useQuery({
    queryKey: ["resume", generation.data?.resumeId],
    queryFn: () => api.get<ResumeResponse>(`/api/resumes/${generation.data!.resumeId}`).then((r) => r.data),
    enabled: !!generation.data?.resumeId,
  });

  async function download(fmt: "pdf" | "docx") {
    const base = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
    const r = await fetch(`${base}/api/generations/${generationId}/${fmt}`, { headers: { Authorization: `Bearer ${token}` } });
    if (!r.ok) return;
    const blob = await r.blob();
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `tailored-resume.${fmt}`;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  if (generation.isLoading) {
    return (
      <div className="blueprint flex items-center justify-center" style={{ minHeight: "calc(100vh - 56px)" }}>
        <div className="flex flex-col items-center gap-3">
          <Loader2 size={22} className="animate-spin" style={{ color: "var(--accent)" }} />
          <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
            loading results…
          </p>
        </div>
      </div>
    );
  }

  if (!generation.data) {
    return (
      <div className="container max-w-md py-24 text-center">
        <h1 className="font-display text-xl font-700" style={{ color: "var(--ink)" }}>
          We couldn't find this result
        </h1>
        <p className="mt-2 text-sm leading-relaxed" style={{ color: "var(--ink-soft)" }}>
          The link may be from another account, or the result was removed.
        </p>
        <AppButton to="/dashboard" variant="secondary" className="mt-6">
          Back to your results
        </AppButton>
      </div>
    );
  }

  const { tailored, ats, baselineScore } = generation.data;
  const gapTerms = new Set(ats.gaps.map((g) => g.term));
  const coverage = [...ats.matchedKeywords.slice(0, 6), ...ats.gaps.slice(0, 6).map((g) => g.term)];
  const sections = source.data ? diffResume(source.data.parsed, tailored) : [];

  return (
    <div className="blueprint" style={{ minHeight: "calc(100vh - 56px)" }}>
      <div style={{ borderBottom: "1px solid var(--hairline)", background: "var(--surface)" }}>
        <div className="container flex h-14 items-center justify-between gap-4">
          <FlowStepper current={2} />
          <AppButton to="/upload" variant="ghost" size="sm">
            + tailor for another job
          </AppButton>
        </div>
      </div>

      {/* ── Instrument panel ───────────────────────────────────── */}
      <section style={{ borderBottom: "1px solid var(--hairline)" }}>
        <div className="container py-9">
          <div className="grid gap-9 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)] lg:gap-14">
            <motion.div
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
              className="rounded-2xl p-6"
              style={{ background: "var(--surface)", border: "1px solid var(--hairline)", boxShadow: "var(--shadow-md)" }}
            >
              <div className="flex items-center gap-5">
                <ScoreRing score={ats.overall} size={112} strokeWidth={10} />
                <div className="min-w-0">
                  {baselineScore != null && baselineScore !== ats.overall && (
                    <p className="font-mono-plex mb-1 text-xs" style={{ color: "var(--ink-faint)" }}>
                      {baselineScore} → <span style={{ color: "var(--good)", fontWeight: 600 }}>{ats.overall}</span>
                    </p>
                  )}
                  <p className="font-display text-xl font-700 leading-tight tracking-tight" style={{ color: "var(--ink)" }}>
                    {scoreVerdict(ats.overall)}
                  </p>
                  <p className="mt-1.5 max-w-xs text-sm leading-relaxed" style={{ color: "var(--ink-soft)" }}>
                    {ats.overall >= 75
                      ? "Covers what this posting asks for. Download it and apply."
                      : ats.overall >= 55
                        ? "Stronger than what you started with. Closing the gaps pushes it further."
                        : "This role asks for a lot your resume doesn't show yet."}
                  </p>
                </div>
              </div>

              <div className="mt-7">
                <AtsScoreBoard ats={ats} />
              </div>

              {coverage.length > 0 && (
                <div className="mt-6 border-t pt-5" style={{ borderColor: "var(--hairline)" }}>
                  <p className="font-mono-plex mb-2.5 text-2xs" style={{ color: "var(--ink-faint)" }}>
                    // keyword coverage
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {coverage.map((term) => (
                      <Chip key={term} term={term} state={gapTerms.has(term) ? "gap" : "found"} />
                    ))}
                  </div>
                </div>
              )}
            </motion.div>

            <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, delay: 0.08 }}>
              <GapList ats={ats} />
            </motion.div>
          </div>
        </div>
      </section>

      {/* ── Output ─────────────────────────────────────────────── */}
      <div className="container py-9">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-4">
          <div className="inline-flex rounded-lg p-1" style={{ background: "var(--surface-sunk)", border: "1px solid var(--hairline)" }}>
            {(["diff", "resume", "letter"] as View[]).map((v) => (
              <button
                key={v}
                onClick={() => setView(v)}
                className="font-mono-plex rounded-md px-3.5 py-1.5 text-xs font-600 transition-colors duration-fast"
                style={v === view ? { background: "var(--surface)", color: "var(--ink)", boxShadow: "var(--shadow-sm)" } : { background: "transparent", color: "var(--ink-faint)" }}
              >
                {v === "diff" ? "diff" : v === "resume" ? "resume" : "cover letter"}
              </button>
            ))}
          </div>

          {view === "resume" && (
            <div className="flex items-center gap-2">
              <AppButton onClick={() => download("docx")} variant="secondary" size="sm">
                <FileText size={13} /> Download DOCX
              </AppButton>
              <AppButton onClick={() => download("pdf")} size="sm">
                <Download size={13} /> Download PDF
              </AppButton>
            </div>
          )}
        </div>

        {view === "diff" ? (
          <motion.div key="diff" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
            {source.isLoading ? (
              <div className="flex items-center gap-2 py-8 text-sm" style={{ color: "var(--ink-faint)" }}>
                <Loader2 size={14} className="animate-spin" />
                loading diff…
              </div>
            ) : (
              <ResumeDiff sections={sections} />
            )}
            <p className="font-mono-plex mt-3 text-2xs" style={{ color: "var(--ink-faint)" }}>
              lines outside this diff were left exactly as you wrote them.{" "}
              <button onClick={() => setView("resume")} className="underline" style={{ color: "var(--accent)" }}>
                see the full resume
              </button>
            </p>
          </motion.div>
        ) : view === "resume" ? (
          <motion.div key="resume" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
            <div className="overflow-hidden rounded-2xl" style={{ border: "1px solid var(--hairline)", boxShadow: "var(--shadow-md)" }}>
              <ResumePreview resume={tailored} />
            </div>
            <p className="mt-3 text-xs" style={{ color: "var(--ink-faint)" }}>
              This is exactly what downloads.{" "}
              <Link to="/upload" style={{ color: "var(--accent)" }}>
                Start another job
              </Link>
              .
            </p>
          </motion.div>
        ) : (
          <motion.div key="letter" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
            <CoverLetterPanel generationId={generationId!} />
          </motion.div>
        )}
      </div>
    </div>
  );
}
