import { motion } from "framer-motion";
import { useAuthStore } from "@/store/auth";
import { useUsage } from "@/lib/usage";
import { useCheckout } from "@/lib/checkout";
import { AppButton } from "@/components/AppButton";

const FREE_FEATURES = ["3 tailors a month", "ATS score with the fix queue", "Cover letter for every run", "PDF and DOCX export"];
const PRO_FEATURES = ["Unlimited tailors", "Unlimited cover letters and rewrites", "ATS score with the fix queue", "PDF and DOCX export", "Every result kept in your history"];

export function Pricing() {
  const user = useAuthStore((s) => s.user);
  const usage = useUsage({ enabled: !!user });
  const checkout = useCheckout();
  const isPro = usage.data?.plan === "PRO";

  return (
    <div className="blueprint" style={{ minHeight: "calc(100vh - 56px)" }}>
      <div className="container py-16 md:py-20">
        <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="mx-auto max-w-2xl text-center">
          <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
            // one plan, no tiers
          </p>
          <h1 className="mt-2 font-display text-4xl font-700 tracking-tight md:text-5xl" style={{ color: "var(--ink)" }}>
            Pay when it earns its keep
          </h1>
          <p className="mx-auto mt-4 max-w-lg text-base leading-relaxed" style={{ color: "var(--ink-soft)" }}>
            Three tailors a month are free, no card needed. Applying to more roles than
            that costs ₹499 for the rest of the month.
          </p>
        </motion.div>

        <div className="mx-auto mt-14 grid max-w-4xl gap-6 md:grid-cols-2">
          {/* Free */}
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 0.05 }}
            className="rounded-2xl p-8"
            style={{ border: "1px solid var(--hairline)", background: "var(--surface)" }}
          >
            <p className="font-mono-plex text-xs" style={{ color: "var(--ink-faint)" }}>
              free
            </p>
            <p className="font-mono-plex mt-3 font-600" style={{ color: "var(--ink)", fontSize: "2.25rem" }}>
              ₹0
            </p>
            <p className="mt-1 text-sm" style={{ color: "var(--ink-faint)" }}>
              3 tailors every month
            </p>

            <FeatureList items={FREE_FEATURES} />

            {user ? (
              <p className="font-mono-plex mt-8 text-center text-xs" style={{ color: "var(--ink-faint)" }}>
                {isPro ? "included in pro" : "this is your current plan"}
              </p>
            ) : (
              <AppButton to="/register" variant="secondary" size="lg" className="mt-8 w-full">
                Start free
              </AppButton>
            )}
          </motion.div>

          {/* Pro — dark card, per the paywall spec */}
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 0.1 }}
            className="blueprint-dark relative rounded-2xl p-8"
            style={{ boxShadow: "var(--shadow-lg)" }}
          >
            <span
              className="font-mono-plex absolute -top-2.5 left-8 rounded-full px-2.5 py-0.5 text-2xs font-600 uppercase tracking-wider"
              style={{ background: "var(--accent)", color: "white" }}
            >
              for active searches
            </span>

            <p className="font-mono-plex text-xs" style={{ color: "var(--accent-on-dark)" }}>
              pro
            </p>
            <p className="font-mono-plex mt-3 font-600" style={{ color: "var(--ink-inverse)", fontSize: "2.25rem" }}>
              ₹499
              <span className="ml-1.5 text-base font-400" style={{ color: "oklch(0.65 0.02 262)" }}>
                /mo
              </span>
            </p>
            <p className="mt-1 text-sm" style={{ color: "oklch(0.65 0.02 262)" }}>
              Cancel any time
            </p>

            <ul className="mt-7 space-y-3">
              {PRO_FEATURES.map((f) => (
                <li key={f} className="flex items-start gap-2.5 text-sm" style={{ color: "oklch(0.8 0.015 262)" }}>
                  <span aria-hidden style={{ color: "var(--accent-on-dark)" }}>✓</span>
                  {f}
                </li>
              ))}
            </ul>

            {isPro ? (
              <p
                className="font-mono-plex mt-8 rounded-lg py-3 text-center text-sm font-600"
                style={{ background: "oklch(1 0 0 / 0.1)", color: "var(--accent-on-dark)" }}
              >
                you're on pro
              </p>
            ) : user ? (
              <AppButton onClick={() => checkout.start()} loading={checkout.isPending} variant="onDark" size="lg" className="mt-8 w-full">
                {checkout.isPending ? "Opening checkout…" : "Upgrade for ₹499/month →"}
              </AppButton>
            ) : (
              <AppButton to="/register" variant="onDark" size="lg" className="mt-8 w-full">
                Create an account
              </AppButton>
            )}

            <p className="mt-3 text-center text-2xs" style={{ color: "oklch(0.55 0.02 262)" }}>
              UPI, cards, netbanking via Razorpay
            </p>
          </motion.div>
        </div>

        <p className="mx-auto mt-10 max-w-md text-center text-xs leading-relaxed" style={{ color: "var(--ink-faint)" }}>
          Your resumes and generated documents stay available on the free plan if you cancel.
        </p>
      </div>
    </div>
  );
}

function FeatureList({ items }: { items: string[] }) {
  return (
    <ul className="mt-7 space-y-3">
      {items.map((f) => (
        <li key={f} className="flex items-start gap-2.5 text-sm" style={{ color: "var(--ink-soft)" }}>
          <span aria-hidden style={{ color: "var(--accent)" }}>✓</span>
          {f}
        </li>
      ))}
    </ul>
  );
}
