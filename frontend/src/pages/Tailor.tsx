import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import axios from "axios";
import { FileText, Lock } from "lucide-react";
import { api, apiErrorMessage } from "@/lib/api";
import { GenerationResponse, ResumeResponse } from "@/types";
import { useToast } from "@/components/ui/toast";
import { QuotaMeter } from "@/components/QuotaMeter";
import { UpgradePanel } from "@/components/UpgradePanel";
import { FlowStepper } from "@/components/FlowStepper";
import { AppButton } from "@/components/AppButton";
import { isExhausted, useUsage } from "@/lib/usage";

const MIN_JD = 50;
const MAX_JD = 20000;

export function Tailor() {
  const { resumeId } = useParams<{ resumeId: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const [jobDescription, setJobDescription] = useState("");
  const [jobTitle, setJobTitle] = useState("");
  const [company, setCompany] = useState("");

  const resume = useQuery({
    queryKey: ["resume", resumeId],
    queryFn: () => api.get<ResumeResponse>(`/api/resumes/${resumeId}`).then((r) => r.data),
    enabled: !!resumeId,
  });

  const usage = useUsage();
  const outOfRuns = isExhausted(usage.data);

  const mutation = useMutation({
    mutationFn: () =>
      api
        .post<GenerationResponse>("/api/generations", {
          resumeId,
          jobDescription,
          jobTitle: jobTitle || null,
          company: company || null,
        })
        .then((r) => r.data),
    onSuccess: (data) => {
      usage.refetch();
      navigate(`/results/${data.id}`);
    },
    onError: (e) => {
      if (axios.isAxiosError(e) && e.response?.status === 402) {
        usage.refetch();
        toast.push({
          title: "That was your last free run this month",
          description: "Upgrade to keep tailoring, or come back when the counter resets.",
          variant: "destructive",
        });
        return;
      }
      toast.push({ title: "Couldn't tailor this one", description: apiErrorMessage(e), variant: "destructive" });
    },
  });

  const jdLen = jobDescription.length;
  const longEnough = jdLen >= MIN_JD && jdLen <= MAX_JD;
  const canRun = longEnough && !outOfRuns;

  return (
    <div className="blueprint" style={{ minHeight: "calc(100vh - 56px)" }}>
      <div style={{ borderBottom: "1px solid var(--hairline)", background: "var(--surface)" }}>
        <div className="container flex h-14 items-center justify-between gap-4">
          <FlowStepper current={1} />
          <p className="font-mono-plex hidden truncate text-xs sm:flex sm:items-center" style={{ color: "var(--ink-faint)" }}>
            <FileText size={12} className="mr-1.5" />
            {resume.data?.originalFilename ?? "loading…"}
          </p>
        </div>
      </div>

      <div className="container py-10">
        <div className="grid gap-10 lg:grid-cols-[1fr_330px]">
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            className="rounded-2xl p-7"
            style={{ background: "var(--surface)", border: "1px solid var(--hairline)", boxShadow: "var(--shadow-md)" }}
          >
            <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
              // step 2 of 3
            </p>
            <h1 className="mt-1.5 font-display text-2xl font-700 tracking-tight" style={{ color: "var(--ink)" }}>
              Paste the job you're applying to
            </h1>
            <p className="mt-1.5 text-sm" style={{ color: "var(--ink-soft)" }}>
              The whole posting works best: requirements, responsibilities, the nice-to-haves.
            </p>

            <div className="mt-6 mb-2 flex items-baseline justify-between">
              <label htmlFor="jd" className="text-sm font-600" style={{ color: "var(--ink)" }}>
                Job description
              </label>
              <span className="font-mono-plex text-xs" style={{ color: jdLen > MAX_JD ? "var(--remove)" : "var(--ink-faint)" }}>
                {jdLen.toLocaleString()}/{MAX_JD.toLocaleString()}
              </span>
            </div>
            <textarea
              id="jd"
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              placeholder="Paste the full job posting here."
              rows={18}
              className="field"
              style={{
                lineHeight: "1.65",
                resize: "vertical",
                borderColor: jdLen > MAX_JD ? "var(--remove)" : "var(--hairline)",
                height: "auto",
                padding: "0.875rem 1rem",
              }}
            />
            {jdLen > 0 && jdLen < MIN_JD && (
              <p className="mt-2 text-xs" style={{ color: "var(--ink-faint)" }}>
                That's shorter than most postings. Add the rest so the score means something.
              </p>
            )}
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 0.05 }}
            className="space-y-4"
          >
            <div
              className="space-y-3 rounded-2xl p-5"
              style={{ background: "var(--surface)", border: "1px solid var(--hairline)" }}
            >
              <SidebarInput label="Job title" placeholder="Senior Backend Engineer" value={jobTitle} onChange={setJobTitle} />
              <SidebarInput label="Company" placeholder="Acme Payments" value={company} onChange={setCompany} />
              <p className="font-mono-plex text-2xs" style={{ color: "var(--ink-faint)" }}>
                optional · both appear in your cover letter
              </p>
            </div>

            {usage.data && !outOfRuns && <QuotaMeter usage={usage.data} compact />}

            {outOfRuns ? (
              <UpgradePanel usage={usage.data!} />
            ) : (
              <>
                <AppButton onClick={() => mutation.mutate()} disabled={!canRun} loading={mutation.isPending} size="lg" className="w-full">
                  {mutation.isPending ? "Scoring…" : "Score my resume →"}
                </AppButton>

                {mutation.isPending && (
                  <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="rounded-lg p-4"
                    style={{ background: "var(--accent-wash)" }}
                  >
                    <p className="font-mono-plex text-xs leading-relaxed" style={{ color: "var(--accent)" }}>
                      // reading the posting, matching against your experience, rewriting bullets. usually 10–30s
                    </p>
                    <div className="mt-3 h-1 overflow-hidden rounded-full" style={{ background: "oklch(0.54 0.16 262 / 0.2)" }}>
                      <motion.div
                        className="h-full rounded-full"
                        style={{ background: "var(--accent)" }}
                        animate={{ width: ["0%", "85%"] }}
                        transition={{ duration: 25, ease: "linear" }}
                      />
                    </div>
                  </motion.div>
                )}
              </>
            )}

            {!outOfRuns && !mutation.isPending && (
              <div className="rounded-2xl p-5" style={{ border: "1px solid var(--hairline)", background: "var(--surface-sunk)" }}>
                <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
                  // what comes back
                </p>
                <ul className="mt-3 space-y-2">
                  {[
                    "A match score for this exact posting",
                    "Missing terms, with point impact",
                    "Bullets rewritten, facts intact",
                    "A one-page cover letter",
                  ].map((s) => (
                    <li key={s} className="flex items-start gap-2 text-xs" style={{ color: "var(--ink-soft)" }}>
                      <span style={{ color: "var(--accent)" }}>→</span>
                      {s}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {outOfRuns && (
              <p className="flex items-start gap-2 text-xs leading-relaxed" style={{ color: "var(--ink-faint)" }}>
                <Lock size={12} className="mt-0.5 shrink-0" />
                Your paste is saved on this screen. Upgrade and it runs straight away.
              </p>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
}

function SidebarInput({ label, placeholder, value, onChange }: { label: string; placeholder: string; value: string; onChange: (v: string) => void }) {
  return (
    <div>
      <label className="font-mono-plex mb-1 block text-2xs" style={{ color: "var(--ink-faint)" }}>
        {label}
      </label>
      <input type="text" value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} className="field" style={{ height: "2.25rem", fontSize: "0.875rem" }} />
    </div>
  );
}
