import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, apiErrorMessage } from "@/lib/api";
import { useToast } from "@/components/ui/toast";

type CheckoutSession = {
  subscriptionId: string;
  keyId: string;
  amountPaise: number;
  currency: string;
  prefillEmail: string;
  prefillName: string;
};

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

const CHECKOUT_SRC = "https://checkout.razorpay.com/v1/checkout.js";

function loadCheckoutScript(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${CHECKOUT_SRC}"]`);
    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("Checkout failed to load")));
      return;
    }
    const script = document.createElement("script");
    script.src = CHECKOUT_SRC;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Checkout failed to load"));
    document.body.appendChild(script);
  });
}

/**
 * Opens Razorpay Checkout for the ₹499 subscription.
 *
 * The browser never grants access: a successful payment here only means Razorpay will send a
 * signed webhook, and that webhook is what upgrades the account. On success we refetch usage
 * rather than assuming the plan changed.
 */
export function useCheckout() {
  const toast = useToast();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: async () => {
      const session = await api.post<CheckoutSession>("/api/billing/checkout").then((r) => r.data);
      await loadCheckoutScript();
      if (!window.Razorpay) throw new Error("Checkout failed to load");

      const rzp = new window.Razorpay({
        key: session.keyId,
        subscription_id: session.subscriptionId,
        name: "Tailored Resume",
        description: "Pro plan, ₹499 per month",
        prefill: { email: session.prefillEmail, name: session.prefillName },
        // Match the Tailr indigo accent (approx of oklch(0.54 0.16 262)), not the old teal.
        theme: { color: "#5a57d1" },
        handler: () => {
          toast.push({
            title: "Payment received",
            description: "Your plan updates as soon as Razorpay confirms it, usually within seconds.",
          });
          // Give the webhook a moment to land, then re-read the plan from our own server.
          setTimeout(() => queryClient.invalidateQueries({ queryKey: ["usage"] }), 3000);
        },
      });
      rzp.open();
    },
    onError: (e) =>
      toast.push({
        title: "Could not start checkout",
        description: apiErrorMessage(e),
        variant: "destructive",
      }),
  });

  return { start: () => mutation.mutate(), isPending: mutation.isPending };
}
