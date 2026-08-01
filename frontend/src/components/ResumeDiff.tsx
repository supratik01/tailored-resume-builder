import { DiffSection } from "@/lib/diffResume";

/** Git-diff-styled panel: dark surface, mono, `-` red wash / `+` green wash. */
export function ResumeDiff({ sections }: { sections: DiffSection[] }) {
  if (sections.length === 0) {
    return (
      <div className="blueprint-dark rounded-2xl p-6 text-center">
        <p className="font-mono-plex text-sm" style={{ color: "oklch(0.68 0.02 262)" }}>
          // no changes — your original bullets already matched
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {sections.map((s) => (
        <div key={s.heading} className="blueprint-dark overflow-hidden rounded-2xl" style={{ border: "1px solid oklch(1 0 0 / 0.08)" }}>
          <div className="flex items-center gap-2 border-b px-4 py-2.5" style={{ borderColor: "oklch(1 0 0 / 0.08)" }}>
            <span className="font-mono-plex text-2xs" style={{ color: "oklch(0.6 0.02 262)" }}>
              {slug(s.heading)}.diff
            </span>
          </div>
          <div className="font-mono-plex px-4 py-3.5 text-[12.5px] leading-relaxed">
            {s.lines.map((line, i) => (
              <p
                key={i}
                className="whitespace-pre-wrap rounded px-2 py-1"
                style={
                  line.type === "removed"
                    ? { background: "var(--remove-wash)", color: "oklch(0.55 0.13 25)" }
                    : line.type === "added"
                      ? { background: "var(--good-wash)", color: "oklch(0.4 0.1 155)" }
                      : { color: "oklch(0.68 0.02 262)" }
                }
              >
                {line.type === "removed" ? "− " : line.type === "added" ? "+ " : "  "}
                {line.text}
              </p>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function slug(s: string): string {
  return s.toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "") || "role";
}
