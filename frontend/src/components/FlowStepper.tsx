/**
 * Step breadcrumb in the top bar: 1 Setup › 2 Score › 3 Tailor.
 * Mono, because the numbering is data about where you are, not decoration.
 */
const STEPS = ["Setup", "Score", "Tailor"] as const;

export function FlowStepper({ current }: { current: 0 | 1 | 2 }) {
  return (
    <nav aria-label="Progress" className="flex items-center gap-1.5 font-mono-plex text-xs">
      {STEPS.map((label, i) => (
        <span key={label} className="flex items-center gap-1.5">
          {i > 0 && (
            <span aria-hidden style={{ color: "var(--hairline-strong)" }}>
              ›
            </span>
          )}
          <span
            className="flex items-center gap-1"
            style={{
              color: i === current ? "var(--accent)" : i < current ? "var(--ink-soft)" : "var(--ink-faint)",
              fontWeight: i === current ? 600 : 400,
            }}
            aria-current={i === current ? "step" : undefined}
          >
            <span style={{ opacity: i === current ? 1 : 0.65 }}>{i + 1}</span>
            <span className="hidden sm:inline">{label}</span>
          </span>
        </span>
      ))}
    </nav>
  );
}
