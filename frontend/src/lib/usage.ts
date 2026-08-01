import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Usage } from "@/types";

/** Free-tier counter. Refetched after every generation so the paywall is never stale. */
export function useUsage(opts: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: ["usage"],
    queryFn: () => api.get<Usage>("/api/billing/usage").then((r) => r.data),
    enabled: opts.enabled ?? true,
  });
}

export function isExhausted(usage: Usage | undefined): boolean {
  return usage?.remaining !== null && usage?.remaining !== undefined && usage.remaining <= 0;
}

export function resetsLabel(usage: Usage | undefined): string {
  if (!usage) return "";
  return new Date(usage.resetsAt).toLocaleDateString(undefined, { day: "numeric", month: "long" });
}
