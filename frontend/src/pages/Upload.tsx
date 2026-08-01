import { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { CloudUpload, CheckCircle2, FileText, Lock } from "lucide-react";
import { api, apiErrorMessage } from "@/lib/api";
import { ResumeResponse } from "@/types";
import { useToast } from "@/components/ui/toast";
import { FlowStepper } from "@/components/FlowStepper";
import { format } from "@/lib/date";

const MAX_MB = 10;

export function Upload() {
  const navigate = useNavigate();
  const toast = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [selected, setSelected] = useState<File | null>(null);
  const [rejected, setRejected] = useState<string | null>(null);

  const resumes = useQuery({
    queryKey: ["resumes"],
    queryFn: () => api.get<ResumeResponse[]>("/api/resumes").then((r) => r.data),
  });

  const mutation = useMutation({
    mutationFn: async (file: File) => {
      const form = new FormData();
      form.append("file", file);
      const r = await api.post<ResumeResponse>("/api/resumes/upload", form, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return r.data;
    },
    onSuccess: (data) => navigate(`/tailor/${data.id}`),
    onError: (e) => {
      setSelected(null);
      toast.push({ title: "Upload failed", description: apiErrorMessage(e), variant: "destructive" });
    },
  });

  function handleFile(file: File | undefined) {
    if (!file) return;
    setRejected(null);
    const ext = file.name.split(".").pop()?.toLowerCase();
    if (ext !== "pdf" && ext !== "docx") {
      setRejected(`${file.name} is a .${ext ?? "?"} file. Save it as PDF or DOCX and try again.`);
      return;
    }
    if (file.size > MAX_MB * 1024 * 1024) {
      const mb = (file.size / 1024 / 1024).toFixed(1);
      setRejected(`${file.name} is ${mb} MB. The limit is ${MAX_MB} MB — export again at lower quality.`);
      return;
    }
    setSelected(file);
    mutation.mutate(file);
  }

  const isPending = mutation.isPending;
  const isSuccess = mutation.isSuccess;
  const previous = resumes.data ?? [];

  return (
    <div className="blueprint" style={{ minHeight: "calc(100vh - 56px)" }}>
      <div style={{ borderBottom: "1px solid var(--hairline)", background: "var(--surface)" }}>
        <div className="container flex h-14 items-center">
          <FlowStepper current={0} />
        </div>
      </div>

      <div className="container max-w-2xl py-12 md:py-16">
        <motion.div
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="rounded-2xl p-8"
          style={{ background: "var(--surface)", border: "1px solid var(--hairline)", boxShadow: "var(--shadow-md)" }}
        >
          <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
            // step 1 of 3
          </p>
          <h1 className="mt-1.5 font-display text-2xl font-700 tracking-tight" style={{ color: "var(--ink)" }}>
            Start with your resume
          </h1>
          <p className="mt-2 text-sm leading-relaxed" style={{ color: "var(--ink-soft)" }}>
            Upload the resume you already send out. Nothing is rewritten here — the next
            step is pasting the job you're applying to.
          </p>

          <label
            className="relative mt-7 flex cursor-pointer flex-col items-center justify-center rounded-xl py-14 text-center transition-all duration-base"
            style={{
              border: `1.5px dashed ${dragging ? "var(--accent)" : "var(--hairline-strong)"}`,
              background: dragging ? "var(--accent-wash)" : "var(--surface-sunk)",
            }}
            onDragEnter={(e) => { e.preventDefault(); setDragging(true); }}
            onDragOver={(e) => e.preventDefault()}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => { e.preventDefault(); setDragging(false); handleFile(e.dataTransfer.files?.[0]); }}
          >
            <AnimatePresence mode="wait">
              {isPending ? (
                <motion.div key="loading" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex flex-col items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full" style={{ background: "var(--accent-wash)" }}>
                    <div className="h-5 w-5 animate-spin rounded-full border-2 border-transparent" style={{ borderTopColor: "var(--accent)", borderRightColor: "var(--accent)" }} />
                  </div>
                  <div>
                    <p className="font-600" style={{ color: "var(--ink)" }}>Reading your resume</p>
                    <p className="font-mono-plex mt-1 text-xs" style={{ color: "var(--ink-faint)" }}>{selected?.name}</p>
                  </div>
                </motion.div>
              ) : isSuccess ? (
                <motion.div key="success" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-center gap-4">
                  <CheckCircle2 size={32} style={{ color: "var(--good)" }} />
                  <p className="font-600" style={{ color: "var(--ink)" }}>Parsed. Opening the next step.</p>
                </motion.div>
              ) : (
                <motion.div key="idle" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full" style={{ background: "var(--surface)" }}>
                    <CloudUpload size={20} style={{ color: "var(--ink-faint)" }} />
                  </div>
                  <div>
                    <p className="font-500" style={{ color: "var(--ink)" }}>Drop your resume here</p>
                    <p className="mt-1 text-sm" style={{ color: "var(--ink-faint)" }}>or click to choose a file</p>
                  </div>
                  <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
                    .pdf .docx · ≤ {MAX_MB}MB
                  </p>
                </motion.div>
              )}
            </AnimatePresence>
            <input ref={inputRef} type="file" accept=".pdf,.docx" className="sr-only" onChange={(e) => handleFile(e.target.files?.[0] ?? undefined)} />
          </label>

          {rejected && (
            <motion.p
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              className="mt-3 rounded-lg px-4 py-3 text-xs leading-relaxed"
              style={{ background: "var(--remove-wash)", color: "oklch(0.5 0.13 25)" }}
            >
              {rejected}
            </motion.p>
          )}

          <p className="font-mono-plex mt-4 flex items-start gap-2 text-xs leading-relaxed" style={{ color: "var(--ink-faint)" }}>
            <Lock size={12} className="mt-0.5 shrink-0" />
            Parsed on this server only — not used to train models, not shared.
          </p>

          {previous.length > 0 && !isPending && !isSuccess && (
            <div className="mt-10 border-t pt-6" style={{ borderColor: "var(--hairline)" }}>
              <p className="font-mono-plex mb-3 text-xs" style={{ color: "var(--ink-faint)" }}>
                // or reuse one you've already uploaded
              </p>
              <ul className="space-y-2">
                {previous.slice(0, 4).map((r) => (
                  <li key={r.id}>
                    <Link
                      to={`/tailor/${r.id}`}
                      className="group flex items-center gap-3 rounded-lg px-3.5 py-2.5 transition-colors duration-fast"
                      style={{ border: "1px solid var(--hairline)", background: "var(--surface)" }}
                      onMouseEnter={(e) => (e.currentTarget.style.borderColor = "var(--hairline-strong)")}
                      onMouseLeave={(e) => (e.currentTarget.style.borderColor = "var(--hairline)")}
                    >
                      <FileText size={14} style={{ color: "var(--ink-faint)" }} />
                      <span className="min-w-0 flex-1 truncate text-sm font-500" style={{ color: "var(--ink)" }}>
                        {r.originalFilename}
                      </span>
                      <span className="font-mono-plex hidden text-xs sm:inline" style={{ color: "var(--ink-faint)" }}>
                        {format(r.createdAt)}
                      </span>
                      <span aria-hidden style={{ color: "var(--accent)" }}>→</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}
