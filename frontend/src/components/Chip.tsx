/**
 * Keyword chips. Mono, because a keyword is data lifted verbatim from the job post.
 * `found` is the good signal, `gap` the warning one; no third decorative state exists.
 */
export function Chip({ term, state }: { term: string; state: "found" | "gap" }) {
  const found = state === "found";
  return (
    <span
      className="font-mono-plex inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-xs"
      style={{
        background: found ? "var(--good-wash)" : "var(--gap-wash)",
        border: `1px solid ${found ? "var(--good)" : "var(--gap)"}`,
        color: found ? "var(--good)" : "oklch(0.5 0.11 70)",
      }}
    >
      <span aria-hidden>{found ? "✓" : "×"}</span>
      {term}
    </span>
  );
}
