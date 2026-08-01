import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { motion } from "framer-motion";
import axios from "axios";
import { Loader2, Mail, Copy, Check, RefreshCw, Download, AlertTriangle } from "lucide-react";
import { api, apiErrorMessage } from "@/lib/api";
import { CoverLetter, CoverLetterTone } from "@/types";
import { useToast } from "@/components/ui/toast";
import { AppButton } from "@/components/AppButton";

const TONES: { value: CoverLetterTone; label: string; hint: string }[] = [
  { value: "professional", label: "professional", hint: "Measured and confident" },
  { value: "warm", label: "warm", hint: "Personable, still precise" },
  { value: "direct", label: "direct", hint: "Short sentences, no filler" },
];

export function CoverLetterPanel({ generationId }: { generationId: string }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tone, setTone] = useState<CoverLetterTone>("professional");
  const [notes, setNotes] = useState("");
  const [copied, setCopied] = useState(false);

  const letter = useQuery({
    queryKey: ["cover-letter", generationId],
    queryFn: async () => {
      try {
        const r = await api.get<CoverLetter>(`/api/generations/${generationId}/cover-letter`);
        return r.data;
      } catch (e) {
        if (axios.isAxiosError(e) && e.response?.status === 404) return null;
        throw e;
      }
    },
  });

  const generate = useMutation({
    mutationFn: () =>
      api.post<CoverLetter>(`/api/generations/${generationId}/cover-letter`, { tone, notes: notes.trim() || null }).then((r) => r.data),
    onSuccess: (data) => {
      queryClient.setQueryData(["cover-letter", generationId], data);
      setTone(data.tone);
    },
    onError: (e) => toast.push({ title: "Cover letter failed", description: apiErrorMessage(e), variant: "destructive" }),
  });

  async function copy(body: string) {
    await navigator.clipboard.writeText(body);
    setCopied(true);
    setTimeout(() => setCopied(false), 1600);
  }

  function downloadTxt(body: string) {
    const blob = new Blob([body], { type: "text/plain;charset=utf-8" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "cover-letter.txt";
    link.click();
    URL.revokeObjectURL(link.href);
  }

  const body = letter.data?.body;
  const busy = generate.isPending;

  return (
    <section>
      <div className="mb-4 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Mail size={14} style={{ color: "var(--accent)" }} />
          <p className="font-mono-plex text-sm font-600" style={{ color: "var(--ink)" }}>
            {body ? "ready to send" : "not written yet"}
          </p>
        </div>
        {body && (
          <div className="flex items-center gap-2">
            <AppButton onClick={() => copy(body)} disabled={busy} variant="secondary" size="sm">
              {copied ? <Check size={13} /> : <Copy size={13} />}
              {copied ? "Copied" : "Copy"}
            </AppButton>
            <AppButton onClick={() => downloadTxt(body)} disabled={busy} variant="secondary" size="sm">
              <Download size={13} /> .txt
            </AppButton>
            <AppButton onClick={() => generate.mutate()} disabled={busy} variant="secondary" size="sm">
              {busy ? <Loader2 size={13} className="animate-spin" /> : <RefreshCw size={13} />}
              Regenerate
            </AppButton>
          </div>
        )}
      </div>

      <div className="rounded-2xl p-6" style={{ border: "1px solid var(--hairline)", background: "var(--surface)" }}>
        {letter.isLoading ? (
          <div className="flex items-center gap-2 py-6 text-sm" style={{ color: "var(--ink-faint)" }}>
            <Loader2 size={15} className="animate-spin" />
            Loading…
          </div>
        ) : body ? (
          <>
            {letter.data!.unsupportedTerms.length > 0 && (
              <div className="mb-5 rounded-lg p-4" style={{ background: "var(--gap-wash)", border: "1px solid var(--gap)" }}>
                <div className="flex items-start gap-2">
                  <AlertTriangle size={14} className="mt-0.5 shrink-0" style={{ color: "oklch(0.5 0.11 70)" }} />
                  <div>
                    <p className="text-xs font-600" style={{ color: "var(--ink)" }}>
                      Check these claims before you send
                    </p>
                    <p className="mt-1 text-xs leading-relaxed" style={{ color: "var(--ink-soft)" }}>
                      The letter mentions {letter.data!.unsupportedTerms.join(", ")}, which your resume
                      does not show. Remove them, or add the experience to your resume if it is real.
                    </p>
                  </div>
                </div>
              </div>
            )}
            <motion.pre
              key={letter.dataUpdatedAt}
              initial={{ opacity: 0, y: 4 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.2 }}
              className="whitespace-pre-wrap text-sm leading-relaxed"
              style={{ color: "var(--ink)", fontFamily: "IBM Plex Sans, sans-serif" }}
            >
              {body}
            </motion.pre>
          </>
        ) : (
          <div className="space-y-5">
            <p className="text-sm leading-relaxed" style={{ color: "var(--ink-soft)" }}>
              Write a one-page letter grounded in the tailored resume above and the job description
              you pasted. Nothing is invented — it only draws on what your resume already shows.
            </p>

            <div>
              <p className="font-mono-plex mb-2 text-2xs" style={{ color: "var(--ink-faint)" }}>
                tone
              </p>
              <div className="flex flex-wrap gap-2">
                {TONES.map((t) => {
                  const active = t.value === tone;
                  return (
                    <button
                      key={t.value}
                      onClick={() => setTone(t.value)}
                      title={t.hint}
                      className="font-mono-plex rounded-md px-3 py-1.5 text-xs font-500 transition-all duration-fast"
                      style={{
                        background: active ? "var(--accent-wash)" : "var(--surface)",
                        color: active ? "var(--accent)" : "var(--ink-faint)",
                        border: `1px solid ${active ? "var(--accent)" : "var(--hairline)"}`,
                      }}
                    >
                      {t.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <label htmlFor="cl-notes" className="font-mono-plex mb-2 block text-2xs" style={{ color: "var(--ink-faint)" }}>
                anything to work in? <span className="normal-case">(optional)</span>
              </label>
              <textarea
                id="cl-notes"
                value={notes}
                onChange={(e) => setNotes(e.target.value.slice(0, 1000))}
                rows={3}
                placeholder="A referral, why this company, a relocation plan…"
                className="field"
                style={{ height: "auto", padding: "0.75rem", lineHeight: "1.6", resize: "vertical" }}
              />
            </div>

            <AppButton onClick={() => generate.mutate()} loading={busy}>
              {busy ? "Writing…" : "Generate cover letter →"}
            </AppButton>
          </div>
        )}
      </div>
    </section>
  );
}
