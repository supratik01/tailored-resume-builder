import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { api, apiErrorMessage } from "@/lib/api";
import { useAuthStore, AuthResponse } from "@/store/auth";
import { useToast } from "@/components/ui/toast";
import { Wordmark } from "@/components/Wordmark";
import { AppButton } from "@/components/AppButton";

const schema = z.object({
  fullName: z.string().min(2, "Enter your name").max(120),
  email: z.string().email("Enter a valid email"),
  password: z.string().min(8, "At least 8 characters").max(128),
});
type Form = z.infer<typeof schema>;

export function Register() {
  const { register, handleSubmit, formState: { errors } } = useForm<Form>({ resolver: zodResolver(schema) });
  const setSession = useAuthStore((s) => s.setSession);
  const navigate = useNavigate();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: (data: Form) => api.post<AuthResponse>("/api/auth/register", data).then((r) => r.data),
    onSuccess: (data) => { setSession(data); navigate("/upload"); },
    onError: (e) => toast.push({ title: "Sign-up failed", description: apiErrorMessage(e), variant: "destructive" }),
  });

  return (
    <div className="flex min-h-[calc(100vh-56px)]">
      <div className="blueprint-dark hidden flex-col justify-between p-12 lg:flex lg:w-[44%]">
        <Wordmark onDark size={20} />
        <div>
          <p className="font-display text-4xl font-700 leading-tight tracking-tight text-balance" style={{ color: "var(--ink-inverse)" }}>
            Stop sending the same resume to every role.
          </p>
          <p className="mt-5 text-base leading-relaxed" style={{ color: "oklch(0.66 0.02 262)" }}>
            Tailr reads the job description, scores the gap between it and your resume, and
            rewrites your bullets to close it — truthfully.
          </p>
          <div className="mt-10 space-y-4">
            {[
              "Upload once. Tailor for any role.",
              "Match score and fix queue on every run.",
              "Exports in PDF and DOCX, ready to send.",
            ].map((item) => (
              <div key={item} className="flex items-start gap-3">
                <span aria-hidden className="mt-0.5 shrink-0 font-mono-plex text-sm" style={{ color: "var(--accent-on-dark)" }}>
                  ✓
                </span>
                <span className="text-sm" style={{ color: "oklch(0.76 0.02 262)" }}>
                  {item}
                </span>
              </div>
            ))}
          </div>
        </div>
        <p className="font-mono-plex text-xs" style={{ color: "oklch(0.4 0.02 262)" }}>
          tailr
        </p>
      </div>

      <div className="flex flex-1 flex-col items-center justify-center px-6 py-16" style={{ background: "var(--surface)" }}>
        <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }} className="w-full max-w-[360px]">
          <h1 className="font-display text-3xl font-700 tracking-tight" style={{ color: "var(--ink)" }}>
            Create an account
          </h1>
          <p className="mt-1.5 text-sm" style={{ color: "var(--ink-soft)" }}>
            Free to start. No credit card required.
          </p>

          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="mt-8 space-y-4">
            <Field label="Full name" error={errors.fullName?.message}>
              <input type="text" autoComplete="name" placeholder="Jane Smith" className="field" {...register("fullName")} />
            </Field>
            <Field label="Email" error={errors.email?.message}>
              <input type="email" autoComplete="email" placeholder="you@example.com" className="field" {...register("email")} />
            </Field>
            <Field label="Password" error={errors.password?.message}>
              <input type="password" autoComplete="new-password" placeholder="At least 8 characters" className="field" {...register("password")} />
            </Field>

            <AppButton type="submit" loading={mutation.isPending} className="w-full">
              {mutation.isPending ? "Creating account…" : "Create account"}
            </AppButton>
          </form>

          <p className="mt-6 text-center text-sm" style={{ color: "var(--ink-soft)" }}>
            Already have one?{" "}
            <Link to="/login" className="font-500" style={{ color: "var(--accent)" }}>
              Sign in
            </Link>
          </p>
        </motion.div>
      </div>
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label className="font-mono-plex text-2xs" style={{ color: "var(--ink-faint)" }}>
        {label}
      </label>
      {children}
      {error && (
        <p className="text-xs" style={{ color: "var(--remove)" }}>
          {error}
        </p>
      )}
    </div>
  );
}
