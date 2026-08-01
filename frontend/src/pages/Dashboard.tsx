import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { FileText, ArrowRight, Clock } from "lucide-react";
import { format } from "@/lib/date";
import { api } from "@/lib/api";
import { GenerationSummary, ResumeResponse } from "@/types";
import { useAuthStore } from "@/store/auth";
import { QuotaMeter } from "@/components/QuotaMeter";
import { AppButton } from "@/components/AppButton";
import { scoreColor } from "@/components/ScoreRing";
import { useUsage } from "@/lib/usage";

const fade = (delay = 0) => ({ initial: { opacity: 0, y: 6 }, animate: { opacity: 1, y: 0 }, transition: { duration: 0.3, delay } });

export function Dashboard() {
  const user = useAuthStore((s) => s.user);

  const resumes = useQuery({ queryKey: ["resumes"], queryFn: () => api.get<ResumeResponse[]>("/api/resumes").then((r) => r.data) });
  const generations = useQuery({ queryKey: ["generations"], queryFn: () => api.get<GenerationSummary[]>("/api/generations").then((r) => r.data) });
  const usage = useUsage();

  return (
    <div className="blueprint" style={{ minHeight: "calc(100vh - 56px)" }}>
      <div style={{ borderBottom: "1px solid var(--hairline)", background: "var(--surface)" }}>
        <div className="container py-8">
          <motion.div {...fade()} className="flex items-end justify-between gap-4">
            <div>
              <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
                // {greeting()}, {user?.fullName?.split(" ")[0] ?? "there"}
              </p>
              <h1 className="mt-0.5 font-display text-3xl font-700 tracking-tight" style={{ color: "var(--ink)" }}>
                Dashboard
              </h1>
            </div>
            <AppButton to="/upload" size="md">
              + Tailor a resume
            </AppButton>
          </motion.div>
        </div>
      </div>

      <div className="container py-8">
        {usage.data && (
          <motion.div {...fade(0.02)} className="mb-8 max-w-md">
            <QuotaMeter usage={usage.data} />
          </motion.div>
        )}

        <div className="grid gap-8 lg:grid-cols-[1fr_400px]">
          <motion.section {...fade(0.05)}>
            <SectionHeader title="Your resumes" count={resumes.data?.length} />
            <div className="mt-4 space-y-2">
              {resumes.isLoading && <SkeletonRows n={3} />}
              {resumes.data?.length === 0 && (
                <EmptyState icon={<FileText size={20} />} title="No resumes yet" action={<Link to="/upload" className="text-sm font-500" style={{ color: "var(--accent)" }}>Upload your first resume</Link>} />
              )}
              {resumes.data?.map((r) => (
                <motion.div
                  key={r.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="group flex items-center gap-4 rounded-lg p-4 transition-colors duration-fast"
                  style={{ border: "1px solid var(--hairline)", background: "var(--surface)" }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = "var(--hairline-strong)")}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = "var(--hairline)")}
                >
                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md" style={{ background: "var(--accent-wash)", color: "var(--accent)" }}>
                    <FileText size={15} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-500" style={{ color: "var(--ink)" }}>
                      {r.originalFilename}
                    </p>
                    <p className="font-mono-plex mt-0.5 text-2xs" style={{ color: "var(--ink-faint)" }}>
                      {r.fileType.toLowerCase()} · {format(r.createdAt)}
                    </p>
                  </div>
                  <Link
                    to={`/tailor/${r.id}`}
                    className="font-mono-plex shrink-0 rounded-md px-3 py-1.5 text-xs font-600 transition-colors duration-fast"
                    style={{ background: "var(--accent-wash)", color: "var(--accent)" }}
                  >
                    tailor
                  </Link>
                </motion.div>
              ))}
            </div>
          </motion.section>

          <motion.section {...fade(0.1)}>
            <SectionHeader title="Recent tailors" count={generations.data?.length} />
            <div className="mt-4 space-y-2">
              {generations.isLoading && <SkeletonRows n={4} />}
              {generations.data?.length === 0 && (
                <EmptyState icon={<Clock size={20} />} title="No tailored resumes yet" action={<span className="text-sm" style={{ color: "var(--ink-soft)" }}>Upload a resume and tailor it to a role.</span>} />
              )}
              {generations.data?.map((g) => (
                <Link
                  to={`/results/${g.id}`}
                  key={g.id}
                  className="group flex items-center gap-4 rounded-lg p-4 transition-colors duration-fast"
                  style={{ border: "1px solid var(--hairline)", background: "var(--surface)" }}
                  onMouseEnter={(e) => ((e.currentTarget as HTMLAnchorElement).style.borderColor = "var(--hairline-strong)")}
                  onMouseLeave={(e) => ((e.currentTarget as HTMLAnchorElement).style.borderColor = "var(--hairline)")}
                >
                  <AtsRing score={g.atsScore} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-500" style={{ color: "var(--ink)" }}>
                      {g.jobTitle ?? "Tailored resume"}
                    </p>
                    <p className="font-mono-plex mt-0.5 text-2xs" style={{ color: "var(--ink-faint)" }}>
                      {g.company ? `${g.company} · ` : ""}
                      {format(g.createdAt)}
                    </p>
                  </div>
                  <ArrowRight size={14} style={{ color: "var(--ink-faint)" }} className="shrink-0 transition-transform duration-fast group-hover:translate-x-0.5" />
                </Link>
              ))}
            </div>
          </motion.section>
        </div>
      </div>
    </div>
  );
}

function AtsRing({ score }: { score: number }) {
  const color = scoreColor(score);
  return (
    <div className="relative flex h-9 w-9 shrink-0 items-center justify-center rounded-full" style={{ border: `2px solid ${color}` }}>
      <span className="font-mono-plex text-xs font-600" style={{ color }}>
        {score}
      </span>
    </div>
  );
}

function SectionHeader({ title, count }: { title: string; count?: number }) {
  return (
    <div className="flex items-baseline gap-2">
      <h2 className="text-base font-600" style={{ color: "var(--ink)" }}>
        {title}
      </h2>
      {count !== undefined && (
        <span className="font-mono-plex rounded-full px-1.5 py-0.5 text-2xs font-600" style={{ background: "var(--surface-sunk)", color: "var(--ink-faint)" }}>
          {count}
        </span>
      )}
    </div>
  );
}

function EmptyState({ icon, title, action }: { icon: React.ReactNode; title: string; action: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg py-10 text-center" style={{ border: "1px dashed var(--hairline)" }}>
      <span style={{ color: "var(--ink-faint)" }}>{icon}</span>
      <p className="text-sm font-500" style={{ color: "var(--ink-soft)" }}>{title}</p>
      {action}
    </div>
  );
}

function SkeletonRows({ n }: { n: number }) {
  return (
    <>
      {Array.from({ length: n }).map((_, i) => (
        <div key={i} className="h-[62px] animate-pulse rounded-lg" style={{ background: "var(--surface-sunk)", border: "1px solid var(--hairline)" }} />
      ))}
    </>
  );
}

function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "morning";
  if (h < 17) return "afternoon";
  return "evening";
}
