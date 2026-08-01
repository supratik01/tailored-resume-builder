import { AtsScore } from "@/types";

/**
 * The fix queue: each row is a keyword the posting leans on, with how many times it
 * appears and how many overall-score points closing it is worth. The point value is
 * server-computed from the real scoring weights, never guessed on the client.
 *
 * Truthfulness is load-bearing: every row is conditional on the candidate actually
 * having done the thing. The product's whole promise is no fabrication.
 */
export function GapList({ ats }: { ats: AtsScore }) {
  const gaps = ats.gaps.slice(0, 8);

  if (gaps.length === 0 && ats.suggestions.length === 0) {
    return (
      <div className="rounded-xl px-5 py-4" style={{ background: "var(--good-wash)", border: "1px solid var(--good)" }}>
        <p className="text-sm font-600" style={{ color: "oklch(0.4 0.1 155)" }}>
          Nothing obvious is missing
        </p>
        <p className="mt-1 text-xs leading-relaxed" style={{ color: "var(--ink-soft)" }}>
          Every term this posting emphasises already appears in your resume. Send it.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {gaps.length > 0 && (
        <div>
          <div className="flex items-baseline justify-between">
            <h3 className="text-sm font-600" style={{ color: "var(--ink)" }}>
              Fix queue
            </h3>
            <span className="font-mono-plex text-2xs" style={{ color: "var(--ink-faint)" }}>
              HIGH → LOW impact
            </span>
          </div>

          <ul className="mt-3 divide-y" style={{ borderColor: "var(--hairline)" }}>
            {gaps.map((g) => (
              <li key={g.term} className="flex items-center gap-3 py-2.5">
                <span
                  className="font-mono-plex shrink-0 rounded px-1.5 py-0.5 text-2xs font-600"
                  style={{ background: "var(--gap-wash)", color: "oklch(0.5 0.11 70)" }}
                >
                  ADD
                </span>
                <span className="min-w-0 flex-1">
                  <span className="font-mono-plex text-sm font-600" style={{ color: "var(--ink)" }}>
                    {g.term}
                  </span>
                  <span className="ml-2 text-xs" style={{ color: "var(--ink-faint)" }}>
                    appears {g.occurrences}× in the post — if you've worked with it
                  </span>
                </span>
                <span className="font-mono-plex shrink-0 text-xs font-600" style={{ color: "var(--good)" }}>
                  +{g.pointsIfAdded} pts
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {ats.suggestions.length > 0 && (
        <div>
          <h3 className="text-sm font-600" style={{ color: "var(--ink)" }}>
            Then tighten these
          </h3>
          <ul className="mt-3 space-y-2">
            {ats.suggestions.map((s, i) => (
              <li key={i} className="flex items-start gap-2.5 text-xs leading-relaxed" style={{ color: "var(--ink-soft)" }}>
                <span className="mt-1 h-1 w-1 shrink-0 rounded-full" style={{ background: "var(--accent)" }} />
                {s}
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className="font-mono-plex text-2xs leading-relaxed" style={{ color: "var(--ink-faint)" }}>
        // anything you haven't actually done stays off the resume — an interview finds it either way
      </p>
    </div>
  );
}
