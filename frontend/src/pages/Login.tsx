import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate, Link } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { api, apiErrorMessage } from "@/lib/api";
import { useAuthStore, AuthResponse } from "@/store/auth";
import { useToast } from "@/components/ui/toast";
import { Wordmark } from "@/components/Wordmark";
import { AppButton } from "@/components/AppButton";

const schema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(8, "At least 8 characters"),
});
type Form = z.infer<typeof schema>;

export function Login() {
  const { register, handleSubmit, formState: { errors } } = useForm<Form>({ resolver: zodResolver(schema) });
  const setSession = useAuthStore((s) => s.setSession);
  const navigate = useNavigate();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: (data: Form) => api.post<AuthResponse>("/api/auth/login", data).then((r) => r.data),
    onSuccess: (data) => { setSession(data); navigate("/dashboard"); },
    onError: (e) => toast.push({ title: "Login failed", description: apiErrorMessage(e), variant: "destructive" }),
  });

  return (
    <div className="flex min-h-[calc(100vh-56px)]">
      <div className="blueprint-dark hidden flex-col justify-between p-12 lg:flex lg:w-[44%]">
        <Wordmark onDark size={20} />
        <div className="space-y-8">
          {[
            { q: "Went from 8% to 34% interview rate in two weeks.", a: "Software engineer, 5 YOE" },
            { q: "Finally stopped guessing what recruiters want to see.", a: "Product manager, fintech" },
            { q: "Saved me hours of manual keyword hunting.", a: "Data scientist, L4" },
          ].map((t, i) => (
            <motion.div key={i} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.12, duration: 0.4 }}>
              <p className="text-base font-500 leading-snug" style={{ color: "var(--ink-inverse)" }}>
                "{t.q}"
              </p>
              <p className="font-mono-plex mt-2 text-xs" style={{ color: "oklch(0.58 0.02 262)" }}>
                {t.a}
              </p>
            </motion.div>
          ))}
        </div>
        <p className="font-mono-plex text-xs" style={{ color: "oklch(0.4 0.02 262)" }}>
          tailr
        </p>
      </div>

      <div className="flex flex-1 flex-col items-center justify-center px-6 py-16" style={{ background: "var(--surface)" }}>
        <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }} className="w-full max-w-[360px]">
          <h1 className="font-display text-3xl font-700 tracking-tight" style={{ color: "var(--ink)" }}>
            Welcome back
          </h1>
          <p className="mt-1.5 text-sm" style={{ color: "var(--ink-soft)" }}>
            Sign in to your account.
          </p>

          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="mt-8 space-y-4">
            <Field label="Email" error={errors.email?.message}>
              <input type="email" autoComplete="email" placeholder="you@example.com" className="field" {...register("email")} />
            </Field>
            <Field label="Password" error={errors.password?.message}>
              <input type="password" autoComplete="current-password" placeholder="••••••••" className="field" {...register("password")} />
            </Field>

            <AppButton type="submit" loading={mutation.isPending} className="w-full">
              {mutation.isPending ? "Signing in…" : "Sign in"}
            </AppButton>
          </form>

          <p className="mt-6 text-center text-sm" style={{ color: "var(--ink-soft)" }}>
            No account?{" "}
            <Link to="/register" className="font-500 transition-colors duration-fast" style={{ color: "var(--accent)" }}>
              Create one free
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
