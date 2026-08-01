import { Route, Routes } from "react-router-dom";
import { Navbar } from "@/components/layout/Navbar";
import { RequireAuth } from "@/components/layout/RequireAuth";
import { Landing } from "@/pages/Landing";
import { Pricing } from "@/pages/Pricing";
import { Login } from "@/pages/Login";
import { Register } from "@/pages/Register";
import { Dashboard } from "@/pages/Dashboard";
import { Upload } from "@/pages/Upload";
import { Tailor } from "@/pages/Tailor";
import { Results } from "@/pages/Results";
import { NotFound } from "@/pages/NotFound";

export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/pricing" element={<Pricing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/dashboard"
            element={
              <RequireAuth>
                <Dashboard />
              </RequireAuth>
            }
          />
          <Route
            path="/upload"
            element={
              <RequireAuth>
                <Upload />
              </RequireAuth>
            }
          />
          <Route
            path="/tailor/:resumeId"
            element={
              <RequireAuth>
                <Tailor />
              </RequireAuth>
            }
          />
          <Route
            path="/results/:generationId"
            element={
              <RequireAuth>
                <Results />
              </RequireAuth>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
      <footer
        className="font-mono-plex py-6 text-center text-2xs"
        style={{ borderTop: "1px solid var(--hairline)", color: "var(--ink-faint)", background: "var(--surface)" }}
      >
        tailr · your resume, rewritten for the job
      </footer>
    </div>
  );
}
