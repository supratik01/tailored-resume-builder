import { AppButton } from "@/components/AppButton";

export function NotFound() {
  return (
    <div className="blueprint flex flex-col items-center justify-center text-center" style={{ minHeight: "calc(100vh - 56px)" }}>
      <span className="font-mono-plex font-700 leading-none" style={{ color: "var(--hairline-strong)", fontSize: "6rem" }}>
        404
      </span>
      <p className="mt-4 text-xl font-600" style={{ color: "var(--ink)" }}>
        Page not found
      </p>
      <p className="mt-2 text-sm" style={{ color: "var(--ink-soft)" }}>
        This URL doesn't exist. Check the address or go back.
      </p>
      <AppButton to="/" size="md" className="mt-8">
        Back to home
      </AppButton>
    </div>
  );
}
