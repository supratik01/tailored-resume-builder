/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    container: {
      center: true,
      padding: { DEFAULT: "1.25rem", md: "2rem", lg: "3rem" },
      screens: { "2xl": "1320px" },
    },
    extend: {
      colors: {
        border:      "hsl(var(--border))",
        input:       "hsl(var(--input))",
        ring:        "hsl(var(--ring))",
        background:  "hsl(var(--background))",
        foreground:  "hsl(var(--foreground))",
        primary: {
          DEFAULT:    "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT:    "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT:    "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT:    "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        destructive: {
          DEFAULT:    "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        card: {
          DEFAULT:    "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        lg:  "var(--radius)",
        md:  "calc(var(--radius) - 2px)",
        sm:  "calc(var(--radius) - 4px)",
        xl:  "1rem",
        "2xl": "1.5rem",
      },
      fontWeight: {
        // Numeric utilities (font-600, font-700…) used throughout — Tailwind's default
        // scale only ships semantic names (font-semibold, font-bold), not numeric ones.
        400: "400",
        500: "500",
        600: "600",
        700: "700",
        800: "800",
      },
      fontFamily: {
        sans:    ["IBM Plex Sans", "system-ui", "-apple-system", "sans-serif"],
        display: ["IBM Plex Sans", "system-ui", "sans-serif"],
        mono:    ["IBM Plex Mono", "ui-monospace", "Menlo", "monospace"],
      },
      fontSize: {
        "2xs":  ["0.6875rem", { lineHeight: "1rem" }],
        xs:     ["0.75rem",   { lineHeight: "1.125rem" }],
        sm:     ["0.875rem",  { lineHeight: "1.375rem" }],
        base:   ["1rem",      { lineHeight: "1.625rem" }],
        lg:     ["1.125rem",  { lineHeight: "1.75rem" }],
        xl:     ["1.25rem",   { lineHeight: "1.875rem" }],
        "2xl":  ["1.5rem",    { lineHeight: "2rem" }],
        "3xl":  ["1.875rem",  { lineHeight: "2.25rem" }],
        "4xl":  ["2.25rem",   { lineHeight: "2.625rem" }],
        "5xl":  ["3rem",      { lineHeight: "3.5rem" }],
        "6xl":  ["3.75rem",   { lineHeight: "4.25rem" }],
        "7xl":  ["4.5rem",    { lineHeight: "5rem" }],
      },
      letterSpacing: {
        tightest: "-0.04em",
        tighter:  "-0.03em",
        tight:    "-0.02em",
        snug:     "-0.01em",
        normal:   "0",
        wide:     "0.02em",
        wider:    "0.06em",
        widest:   "0.12em",
      },
      boxShadow: {
        sm:  "var(--shadow-sm)",
        DEFAULT: "var(--shadow-md)",
        md:  "var(--shadow-md)",
        lg:  "var(--shadow-lg)",
        none: "none",
      },
      transitionDuration: {
        fast: "150ms",
        base: "200ms",
        slow: "350ms",
      },
      animation: {
        "spin-slow": "spin 3s linear infinite",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};
