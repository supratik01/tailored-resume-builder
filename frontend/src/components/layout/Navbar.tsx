import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuthStore } from "@/store/auth";
import { Wordmark } from "@/components/Wordmark";
import { useUsage } from "@/lib/usage";

export function Navbar() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();
  const location = useLocation();
  const isLanding = location.pathname === "/";
  const [atTop, setAtTop] = useState(true);
  const usage = useUsage({ enabled: !!user });

  // Watch a 1px sentinel at the very top of the page instead of listening to
  // every scroll frame. While it is on screen we are at the top of the hero.
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    if (!isLanding) {
      setAtTop(false);
      return;
    }
    // A 24px probe pinned to the top of the page. While any of it is on screen we
    // are within the first 24px of scroll and the nav floats; once it scrolls out
    // the nav goes solid. No per-frame scroll work.
    const el = document.createElement("div");
    el.style.cssText = "position:absolute;top:0;height:24px;width:1px;pointer-events:none";
    document.body.appendChild(el);
    sentinelRef.current = el;
    const io = new IntersectionObserver(([entry]) => setAtTop(entry.isIntersecting), {
      threshold: 0,
    });
    io.observe(el);
    return () => {
      io.disconnect();
      el.remove();
    };
  }, [isLanding]);

  // The landing hero is the light blueprint canvas, so the "floating" nav stays
  // dark-on-light. Only the border and shadow fade in once the user scrolls.
  const floating = isLanding && atTop;

  function onLogout() {
    logout();
    navigate("/");
  }

  return (
    <header
      style={{
        position: "sticky",
        top: 0,
        zIndex: 40,
        transition: "background 200ms ease, border-color 200ms ease, box-shadow 200ms ease",
        background: floating ? "transparent" : "var(--surface)",
        borderBottom: floating ? "1px solid transparent" : "1px solid var(--hairline)",
        boxShadow: floating ? "none" : "var(--shadow-sm)",
      }}
    >
      <div className="container flex h-14 items-center justify-between">
        <Link to={user ? "/dashboard" : "/"}>
          <Wordmark size={21} />
        </Link>

        <nav className="flex items-center gap-1">
          {user ? (
            <>
              <NavLink to="/dashboard">Dashboard</NavLink>
              <span className="hidden sm:contents">
                <NavLink to="/upload">Upload</NavLink>
                <NavLink to="/pricing">Pricing</NavLink>
              </span>

              {usage.data && (
                <span
                  className="font-mono-plex ml-2 hidden items-center gap-1 rounded-md px-2 py-1 text-xs md:inline-flex"
                  style={
                    usage.data.plan === "PRO"
                      ? { background: "var(--accent-wash)", color: "var(--accent)" }
                      : { background: "var(--surface-sunk)", color: "var(--ink-faint)", border: "1px solid var(--hairline)" }
                  }
                >
                  {usage.data.plan === "PRO" ? "pro · unlimited" : `${usage.data.remaining}/${usage.data.limit} runs`}
                </span>
              )}

              <div className="mx-2 h-4 w-px" style={{ background: "var(--hairline)" }} />
              <span className="hidden text-xs md:inline" style={{ color: "var(--ink-faint)" }}>
                {user.email}
              </span>
              <button
                onClick={onLogout}
                className="ml-2 whitespace-nowrap rounded-md px-3.5 py-1.5 text-sm font-medium transition-colors duration-fast"
                style={{ background: "var(--surface-sunk)", color: "var(--ink)" }}
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/pricing">Pricing</NavLink>
              <NavLink to="/login">Log in</NavLink>
              <Link
                to="/register"
                className="ml-1 whitespace-nowrap rounded-md px-4 py-1.5 text-sm font-semibold transition-all duration-fast"
                style={{ background: "var(--accent)", color: "white", border: "1px solid var(--accent)" }}
              >
                Get started
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}

function NavLink({ to, children }: { to: string; children: React.ReactNode }) {
  const location = useLocation();
  const active = location.pathname === to;
  return (
    <Link
      to={to}
      className="rounded-md px-3 py-1.5 text-sm font-medium transition-colors duration-fast"
      style={{
        color: active ? "var(--accent)" : "var(--ink-soft)",
        background: active ? "var(--accent-wash)" : "transparent",
      }}
    >
      {children}
    </Link>
  );
}
