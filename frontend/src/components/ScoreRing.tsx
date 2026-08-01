import { motion } from "framer-motion";

/**
 * Signal colour by band. Used everywhere a score is read, so the same number never
 * means two different things on two screens.
 */
export function scoreColor(v: number): string {
  if (v >= 75) return "var(--good)";
  if (v >= 55) return "var(--gap)";
  return "var(--remove)";
}

export function scoreVerdict(v: number): string {
  if (v >= 75) return "Strong match";
  if (v >= 55) return "Close, with gaps";
  return "Big gaps to close";
}

/**
 * The instrument. Track is hairline, fill is the accent (or a signal colour when the
 * score itself is the message). Number sits in Plex Mono.
 */
export function ScoreRing({
  score,
  size = 128,
  strokeWidth = 12,
  label = "match",
  animate = true,
  onDark = false,
  tone = "signal",
}: {
  score: number;
  size?: number;
  strokeWidth?: number;
  label?: string;
  animate?: boolean;
  onDark?: boolean;
  tone?: "signal" | "accent";
}) {
  const r = size / 2 - strokeWidth / 2 - 2;
  const c = size / 2;
  const circ = 2 * Math.PI * r;
  const dash = (score / 100) * circ;
  const fill = tone === "accent" ? "var(--accent)" : scoreColor(score);

  return (
    <div className="relative inline-flex shrink-0 items-center justify-center">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle
          cx={c}
          cy={c}
          r={r}
          fill="none"
          stroke={onDark ? "oklch(1 0 0 / 0.14)" : "var(--hairline)"}
          strokeWidth={strokeWidth}
        />
        <motion.circle
          cx={c}
          cy={c}
          r={r}
          fill="none"
          stroke={fill}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circ}
          initial={{ strokeDashoffset: animate ? circ : circ - dash }}
          animate={{ strokeDashoffset: circ - dash }}
          transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1] }}
          style={{ transformOrigin: "center", transform: "rotate(-90deg)" }}
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span
          className="font-mono-plex font-600 leading-none"
          style={{ color: onDark ? "var(--ink-inverse)" : "var(--ink)", fontSize: Math.round(size * 0.28) }}
        >
          {score}
        </span>
        <span
          className="font-mono-plex mt-1 uppercase"
          style={{
            color: onDark ? "oklch(0.72 0.02 262)" : "var(--ink-faint)",
            fontSize: Math.max(9, Math.round(size * 0.078)),
            letterSpacing: "0.08em",
          }}
        >
          {label}
        </span>
      </div>
    </div>
  );
}
