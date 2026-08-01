import { ParsedResume } from "@/types";

export type DiffLine = { type: "removed" | "added" | "unchanged"; text: string };
export type DiffSection = { heading: string; lines: DiffLine[] };

/**
 * A resume-level diff, not a generic text diff. It compares the source resume the
 * candidate uploaded against the tailored one, section by section, so the change is
 * traceable to a real before/after rather than an algorithmic line match.
 *
 * Experience roles line up by index (tailoring reorders occasionally but rarely
 * inserts new roles); bullets within a role line up by index too. A bullet whose text
 * changed renders as a removed line followed by an added line; identical bullets render
 * unchanged so the diff doesn't imply a rewrite that didn't happen.
 */
export function diffResume(source: ParsedResume, tailored: ParsedResume): DiffSection[] {
  const sections: DiffSection[] = [];

  if (source.summary !== tailored.summary) {
    sections.push({
      heading: "summary",
      lines: [
        ...(source.summary ? [{ type: "removed" as const, text: source.summary }] : []),
        ...(tailored.summary ? [{ type: "added" as const, text: tailored.summary }] : []),
      ],
    });
  }

  const roleCount = Math.max(source.experience?.length ?? 0, tailored.experience?.length ?? 0);
  for (let i = 0; i < roleCount; i++) {
    const srcRole = source.experience?.[i];
    const tgtRole = tailored.experience?.[i];
    if (!tgtRole) continue;

    const lines: DiffLine[] = [];
    const bulletCount = Math.max(srcRole?.bullets?.length ?? 0, tgtRole.bullets?.length ?? 0);
    for (let j = 0; j < bulletCount; j++) {
      const srcBullet = srcRole?.bullets?.[j];
      const tgtBullet = tgtRole.bullets?.[j];
      if (srcBullet && tgtBullet && srcBullet === tgtBullet) {
        lines.push({ type: "unchanged", text: tgtBullet });
      } else {
        if (srcBullet) lines.push({ type: "removed", text: srcBullet });
        if (tgtBullet) lines.push({ type: "added", text: tgtBullet });
      }
    }
    if (lines.some((l) => l.type !== "unchanged")) {
      sections.push({ heading: tgtRole.title || srcRole?.title || `role ${i + 1}`, lines });
    }
  }

  return sections;
}
