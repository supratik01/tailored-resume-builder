/**
 * The caret in a rounded square reads terminal/git; the lowercase mono name keeps the
 * dev-tool voice. Used in the top bar and on the landing hero.
 */
export function Wordmark({ onDark = false, size = 22 }: { onDark?: boolean; size?: number }) {
  return (
    <span className="flex shrink-0 items-center gap-2">
      <span
        className="flex items-center justify-center font-mono-plex font-600 leading-none"
        style={{
          width: size,
          height: size,
          borderRadius: Math.round(size * 0.27),
          background: "var(--accent)",
          color: "white",
          fontSize: Math.round(size * 0.62),
          paddingBottom: 1,
        }}
        aria-hidden
      >
        ›
      </span>
      <span
        className="font-mono-plex font-600 lowercase"
        style={{
          fontSize: Math.round(size * 0.75),
          letterSpacing: "-0.02em",
          color: onDark ? "var(--ink-inverse)" : "var(--ink)",
        }}
      >
        tailr
      </span>
    </span>
  );
}
