import { Usage } from "@/types";
import { resetsLabel } from "@/lib/usage";
import { useCheckout } from "@/lib/checkout";
import { AppButton } from "@/components/AppButton";

const INCLUDED = ["Unlimited tailors", "Unlimited cover letters", "Everything you've made stays yours"];

/**
 * Shown once the month's free runs are spent. Dark plan card per the paywall spec: a
 * soft next step, not a wall. Leads with the value already received.
 */
export function UpgradePanel({ usage }: { usage: Usage }) {
  const checkout = useCheckout();
  const used = usage.limit ?? usage.used;

  return (
    <div className="overflow-hidden rounded-2xl" style={{ border: "1px solid var(--hairline)" }}>
      <div className="px-5 py-4" style={{ background: "var(--gap-wash)" }}>
        <p className="font-mono-plex text-xs" style={{ color: "oklch(0.5 0.11 70)" }}>
          {used} of {used} free tailors used
        </p>
        <p className="mt-1 text-sm font-500" style={{ color: "var(--ink)" }}>
          Refills on {resetsLabel(usage)}, or upgrade now.
        </p>
      </div>

      <div className="blueprint-dark p-5">
        <p className="font-mono-plex font-600" style={{ color: "var(--ink-inverse)", fontSize: "1.75rem" }}>
          ₹499
          <span className="ml-1 text-sm font-400" style={{ color: "oklch(0.65 0.02 262)" }}>
            /mo
          </span>
        </p>
        <p className="mt-1 text-xs" style={{ color: "oklch(0.65 0.02 262)" }}>
          Cancel anytime. No tiers, one plan.
        </p>

        <ul className="mt-4 space-y-2">
          {INCLUDED.map((f) => (
            <li key={f} className="flex items-start gap-2 text-xs" style={{ color: "oklch(0.78 0.015 262)" }}>
              <span aria-hidden style={{ color: "var(--accent-on-dark)" }}>✓</span>
              {f}
            </li>
          ))}
        </ul>

        <AppButton onClick={() => checkout.start()} loading={checkout.isPending} variant="onDark" className="mt-5 w-full">
          {checkout.isPending ? "Opening checkout…" : "Upgrade for ₹499/month →"}
        </AppButton>

        <p className="mt-3 text-center text-2xs" style={{ color: "oklch(0.55 0.02 262)" }}>
          UPI, cards, netbanking via Razorpay
        </p>
      </div>
    </div>
  );
}
