import { motion } from "framer-motion";
import { AtsScore } from "@/types";
import { scoreColor } from "@/components/ScoreRing";

const SUBSCORES: Array<{ key: keyof AtsScore; label: string }> = [
  { key: "keywordMatch", label: "keyword_match" },
  { key: "skillAlignment", label: "skill_alignment" },
  { key: "formattingQuality", label: "formatting" },
  { key: "readability", label: "readability" },
];

/** The four component metrics behind the headline score, read like an instrument. */
export function AtsScoreBoard({ ats }: { ats: AtsScore }) {
  return (
    <div className="space-y-3.5">
      {SUBSCORES.map(({ key, label }, i) => {
        const value = ats[key] as number;
        const color = scoreColor(value);
        return (
          <div key={key} className="flex items-center gap-3">
            <span className="font-mono-plex w-32 shrink-0 text-xs" style={{ color: "var(--ink-faint)" }}>
              {label}
            </span>
            <div className="h-1.5 flex-1 overflow-hidden rounded-full" style={{ background: "var(--hairline)" }}>
              <motion.div
                className="h-full rounded-full"
                style={{ background: color }}
                initial={{ width: 0 }}
                animate={{ width: `${value}%` }}
                transition={{ duration: 0.6, delay: 0.08 * i, ease: [0.22, 1, 0.36, 1] }}
              />
            </div>
            <span className="font-mono-plex w-8 shrink-0 text-right text-xs font-600" style={{ color }}>
              {value}
            </span>
          </div>
        );
      })}
    </div>
  );
}
