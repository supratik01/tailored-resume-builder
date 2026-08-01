import { Usage } from "@/types";
import { isExhausted, resetsLabel } from "@/lib/usage";

/** Compact usage readout for the tailor sidebar. Full paywall state lives in UpgradePanel. */
export function QuotaMeter({ usage, compact = false }: { usage: Usage; compact?: boolean }) {
  if (usage.plan === "PRO") {
    return (
      <div
        className="font-mono-plex flex items-center gap-2 rounded-lg px-3.5 py-2.5 text-xs"
        style={{ background: "var(--accent-wash)", border: "1px solid var(--accent)", color: "var(--accent)" }}
      >
        pro · unlimited tailors
      </div>
    );
  }

  const limit = usage.limit ?? 0;
  const used = Math.min(usage.used, limit);
  const out = isExhausted(usage);

  return (
    <div
      className="rounded-lg p-3.5"
      style={{
        background: out ? "var(--gap-wash)" : "var(--surface-sunk)",
        border: `1px solid ${out ? "var(--gap)" : "var(--hairline)"}`,
      }}
    >
      <div className="flex items-baseline justify-between gap-3">
        <p className="font-mono-plex text-xs font-600" style={{ color: "var(--ink)" }}>
          {out ? "0 runs left" : `${usage.remaining}/${limit} runs left`}
        </p>
        {!compact && (
          <p className="font-mono-plex text-2xs" style={{ color: "var(--ink-faint)" }}>
            resets {resetsLabel(usage)}
          </p>
        )}
      </div>
      <div className="mt-2.5 flex gap-1">
        {Array.from({ length: limit }).map((_, i) => (
          <div
            key={i}
            className="h-1.5 flex-1 rounded-full"
            style={{ background: i < used ? (out ? "var(--gap)" : "var(--accent)") : "var(--hairline-strong)" }}
          />
        ))}
      </div>
    </div>
  );
}
