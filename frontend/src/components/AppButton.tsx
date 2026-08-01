import * as React from "react";
import { Link } from "react-router-dom";
import { Loader2 } from "lucide-react";

/**
 * The one button vocabulary. Primary is accent fill; secondary is surface plus hairline.
 * Labels always say exactly what the button does, and carry an affordance glyph (→ ↓)
 * rather than a decorative icon.
 */
type Variant = "primary" | "secondary" | "ghost" | "onDark";
type Size = "sm" | "md" | "lg";

const sizeStyles: Record<Size, React.CSSProperties> = {
  sm: { height: "2rem", padding: "0 0.75rem", fontSize: "0.8125rem", borderRadius: "7px", gap: "0.4rem" },
  md: { height: "2.375rem", padding: "0 1rem", fontSize: "0.875rem", borderRadius: "8px", gap: "0.45rem" },
  lg: { height: "2.75rem", padding: "0 1.375rem", fontSize: "0.9375rem", borderRadius: "9px", gap: "0.5rem" },
};

function variantStyles(v: Variant): React.CSSProperties {
  switch (v) {
    case "primary":
      return { background: "var(--accent)", color: "white", border: "1px solid transparent" };
    case "secondary":
      return { background: "var(--surface)", color: "var(--ink)", border: "1px solid var(--hairline)" };
    case "ghost":
      return { background: "transparent", color: "var(--ink-soft)", border: "1px solid transparent" };
    case "onDark":
      return { background: "var(--surface)", color: "var(--dark-panel)", border: "1px solid transparent" };
  }
}

function hoverStyles(v: Variant): React.CSSProperties {
  switch (v) {
    case "primary": return { background: "var(--accent-hover)" };
    case "secondary": return { background: "var(--surface-sunk)", borderColor: "var(--hairline-strong)" };
    case "ghost": return { background: "var(--surface-sunk)" };
    case "onDark": return { background: "oklch(0.93 0.01 260)" };
  }
}

type Common = {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
};

type BtnProps = Common & React.ButtonHTMLAttributes<HTMLButtonElement> & { to?: undefined };
type LinkProps = Common & { to: string };

export function AppButton(props: BtnProps | LinkProps) {
  const { variant = "primary", size = "md", loading, children, className, style } = props;

  const base: React.CSSProperties = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    fontWeight: 600,
    letterSpacing: "-0.01em",
    whiteSpace: "nowrap",
    cursor: "pointer",
    transition: "background 150ms ease, border-color 150ms ease, opacity 150ms ease, transform 120ms ease",
    ...sizeStyles[size],
    ...variantStyles(variant),
    ...style,
  };

  // Tactile press: a small physical push on :active. Direct-manipulation feedback,
  // disabled buttons stay still. Class is gated so reduced-motion users keep it too
  // (a press cue is affordance, not decorative motion).
  const pressClass = "motion-safe:active:scale-[0.98]";

  const enter = (e: React.MouseEvent<HTMLElement>) => Object.assign(e.currentTarget.style, hoverStyles(variant));
  const leave = (e: React.MouseEvent<HTMLElement>) => Object.assign(e.currentTarget.style, variantStyles(variant));

  const content = (
    <>
      {loading && <Loader2 size={size === "sm" ? 13 : 15} className="animate-spin" />}
      {children}
    </>
  );

  if ("to" in props && props.to) {
    return (
      <Link
        to={props.to}
        className={`focus-ring ${pressClass} ${className ?? ""}`}
        style={base}
        onMouseEnter={enter}
        onMouseLeave={leave}
      >
        {content}
      </Link>
    );
  }

  const { to: _t, variant: _v, size: _s, loading: _l, ...rest } = props as BtnProps;
  const disabled = rest.disabled || loading;

  return (
    <button
      {...rest}
      disabled={disabled}
      className={`focus-ring ${disabled ? "" : pressClass} ${className ?? ""}`}
      style={{ ...base, ...(disabled ? { opacity: 0.5, cursor: "not-allowed" } : {}) }}
      onMouseEnter={disabled ? undefined : enter}
      onMouseLeave={disabled ? undefined : leave}
    >
      {content}
    </button>
  );
}
