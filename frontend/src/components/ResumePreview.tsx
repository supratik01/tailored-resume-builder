import { ParsedResume } from "@/types";

export function ResumePreview({ resume: r }: { resume: ParsedResume }) {
  const c = r.contact;
  const contactLine = [c?.email, c?.phone, c?.location, c?.linkedin, c?.website]
    .filter(Boolean)
    .join(" · ");

  return (
    <article
      style={{
        background: "white",
        color: "#111",
        fontFamily: "'Helvetica Neue', 'Helvetica', 'Arial', sans-serif",
        fontSize: "10.5pt",
        lineHeight: "1.42",
        padding: "2.5rem 2.75rem",
        maxWidth: "860px",
        margin: "0 auto",
      }}
    >
      {/* Header */}
      <header style={{ marginBottom: "1.5rem", paddingBottom: "0.875rem", borderBottom: "1.5px solid #111" }}>
        <h1 style={{ fontSize: "18pt", fontWeight: 700, letterSpacing: "0.3pt", margin: "0 0 4pt 0", color: "#0d0d0d" }}>
          {c?.fullName ?? ""}
        </h1>
        {contactLine && (
          <p style={{ fontSize: "9pt", color: "#555", margin: 0, letterSpacing: "0.1pt" }}>
            {contactLine}
          </p>
        )}
      </header>

      {/* Summary */}
      {r.summary && (
        <Section title="Summary">
          <p style={{ margin: 0 }}>{r.summary}</p>
        </Section>
      )}

      {/* Skills */}
      {r.skills?.length > 0 && (
        <Section title="Skills">
          <p style={{ margin: 0, lineHeight: "1.6" }}>{r.skills.join(" · ")}</p>
        </Section>
      )}

      {/* Experience */}
      {r.experience?.length > 0 && (
        <Section title="Experience">
          {r.experience.map((e, i) => (
            <div key={i} style={{ marginBottom: i < r.experience.length - 1 ? "10pt" : 0 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                <span style={{ fontWeight: 600, fontSize: "10.5pt" }}>
                  {e.title}
                  {e.company && (
                    <span style={{ fontWeight: 400, color: "#444" }}> — {e.company}</span>
                  )}
                </span>
                <span style={{ fontSize: "9pt", color: "#666", whiteSpace: "nowrap", marginLeft: "12pt" }}>
                  {[e.startDate, e.endDate].filter(Boolean).join(" – ")}
                </span>
              </div>
              {e.bullets?.length > 0 && (
                <ul style={{ margin: "3pt 0 0 14pt", padding: 0, listStyleType: "disc" }}>
                  {e.bullets.map((b, j) => (
                    <li key={j} style={{ marginBottom: "2pt" }}>{b}</li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </Section>
      )}

      {/* Projects */}
      {r.projects?.length > 0 && (
        <Section title="Projects">
          {r.projects.map((p, i) => (
            <div key={i} style={{ marginBottom: i < r.projects.length - 1 ? "10pt" : 0 }}>
              <span style={{ fontWeight: 600 }}>
                {p.name}
                {p.tech?.length > 0 && (
                  <span style={{ fontWeight: 400, color: "#555", fontStyle: "italic" }}>
                    {" "}— {p.tech.join(", ")}
                  </span>
                )}
              </span>
              {p.description && <p style={{ margin: "2pt 0 0 0" }}>{p.description}</p>}
              {p.bullets?.length > 0 && (
                <ul style={{ margin: "3pt 0 0 14pt", padding: 0, listStyleType: "disc" }}>
                  {p.bullets.map((b, j) => (
                    <li key={j} style={{ marginBottom: "2pt" }}>{b}</li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </Section>
      )}

      {/* Education */}
      {r.education?.length > 0 && (
        <Section title="Education">
          {r.education.map((ed, i) => (
            <div
              key={i}
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: i < r.education.length - 1 ? "6pt" : 0,
              }}
            >
              <span style={{ fontWeight: 600 }}>
                {ed.institution}
                {ed.degree && <span style={{ fontWeight: 400 }}> — {ed.degree}</span>}
                {ed.field  && <span style={{ fontWeight: 400, color: "#555" }}>, {ed.field}</span>}
              </span>
              <span style={{ fontSize: "9pt", color: "#666", whiteSpace: "nowrap", marginLeft: "12pt" }}>
                {[ed.startDate, ed.endDate].filter(Boolean).join(" – ")}
              </span>
            </div>
          ))}
        </Section>
      )}

      {/* Certifications */}
      {r.certifications?.length > 0 && (
        <Section title="Certifications">
          <ul style={{ margin: "0 0 0 14pt", padding: 0, listStyleType: "disc" }}>
            {r.certifications.map((cert, i) => (
              <li key={i} style={{ marginBottom: "2pt" }}>{cert}</li>
            ))}
          </ul>
        </Section>
      )}
    </article>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section style={{ marginBottom: "1rem" }}>
      <h2
        style={{
          fontSize: "7.5pt",
          fontWeight: 700,
          textTransform: "uppercase",
          letterSpacing: "1.4pt",
          color: "#111",
          borderBottom: "0.75pt solid #111",
          paddingBottom: "2pt",
          marginBottom: "7pt",
          margin: "0 0 6pt 0",
        }}
      >
        {title}
      </h2>
      {children}
    </section>
  );
}
