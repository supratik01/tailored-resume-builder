package com.tailored.resume.export;

import com.tailored.resume.dto.resume.ParsedResume;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Component
public class ResumeHtmlRenderer {

    public String render(ParsedResume r) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("""
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="UTF-8"/>
                <title>Resume</title>
                <style>
                  @page { size: Letter; margin: 0.5in 0.6in; }
                  * { box-sizing: border-box; }
                  body { font-family: 'Helvetica', 'Arial', sans-serif; color: #111; font-size: 10.5pt; line-height: 1.35; margin: 0; }
                  h1 { font-size: 18pt; margin: 0 0 2pt 0; letter-spacing: 0.5pt; }
                  .contact { font-size: 9.5pt; color: #333; margin-bottom: 8pt; }
                  h2 { font-size: 11pt; text-transform: uppercase; letter-spacing: 1.2pt; border-bottom: 1pt solid #111; padding-bottom: 2pt; margin: 10pt 0 4pt 0; }
                  .role-header { display: block; margin-top: 4pt; }
                  .role-title { font-weight: bold; }
                  .role-meta { float: right; color: #444; font-size: 9.5pt; }
                  .role-company { font-style: italic; color: #333; }
                  ul { margin: 2pt 0 2pt 14pt; padding: 0; }
                  li { margin-bottom: 1pt; }
                  .skills { line-height: 1.5; }
                  p { margin: 2pt 0; }
                  .clearfix::after { content: ""; display: table; clear: both; }
                </style></head><body>
                """);

        var c = r.contact();
        sb.append("<h1>").append(safe(c == null ? null : c.fullName())).append("</h1>");
        sb.append("<div class=\"contact\">");
        sb.append(joinContact(c));
        sb.append("</div>");

        if (notBlank(r.summary())) {
            sb.append("<h2>Summary</h2><p>").append(safe(r.summary())).append("</p>");
        }
        if (r.skills() != null && !r.skills().isEmpty()) {
            sb.append("<h2>Skills</h2><p class=\"skills\">").append(safe(String.join(" • ", r.skills()))).append("</p>");
        }
        if (r.experience() != null && !r.experience().isEmpty()) {
            sb.append("<h2>Experience</h2>");
            for (var e : r.experience()) {
                sb.append("<div class=\"role-header clearfix\">");
                sb.append("<span class=\"role-meta\">").append(safe(joinDates(e.startDate(), e.endDate()))).append("</span>");
                sb.append("<span class=\"role-title\">").append(safe(e.title())).append("</span>");
                if (notBlank(e.company())) sb.append(" — <span class=\"role-company\">").append(safe(e.company())).append("</span>");
                if (notBlank(e.location())) sb.append(" (").append(safe(e.location())).append(")");
                sb.append("</div>");
                writeBullets(sb, e.bullets());
            }
        }
        if (r.projects() != null && !r.projects().isEmpty()) {
            sb.append("<h2>Projects</h2>");
            for (var p : r.projects()) {
                sb.append("<div class=\"role-header\"><span class=\"role-title\">").append(safe(p.name())).append("</span>");
                if (p.tech() != null && !p.tech().isEmpty()) {
                    sb.append(" <span class=\"role-company\">— ").append(safe(String.join(", ", p.tech()))).append("</span>");
                }
                sb.append("</div>");
                if (notBlank(p.description())) sb.append("<p>").append(safe(p.description())).append("</p>");
                writeBullets(sb, p.bullets());
            }
        }
        if (r.education() != null && !r.education().isEmpty()) {
            sb.append("<h2>Education</h2>");
            for (var ed : r.education()) {
                sb.append("<div class=\"role-header clearfix\">");
                sb.append("<span class=\"role-meta\">").append(safe(joinDates(ed.startDate(), ed.endDate()))).append("</span>");
                sb.append("<span class=\"role-title\">").append(safe(ed.institution())).append("</span>");
                if (notBlank(ed.degree())) sb.append(" — ").append(safe(ed.degree()));
                if (notBlank(ed.field())) sb.append(", ").append(safe(ed.field()));
                sb.append("</div>");
                if (notBlank(ed.details())) sb.append("<p>").append(safe(ed.details())).append("</p>");
            }
        }
        if (r.certifications() != null && !r.certifications().isEmpty()) {
            sb.append("<h2>Certifications</h2><ul>");
            for (String cert : r.certifications()) sb.append("<li>").append(safe(cert)).append("</li>");
            sb.append("</ul>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private void writeBullets(StringBuilder sb, List<String> bullets) {
        if (bullets == null || bullets.isEmpty()) return;
        sb.append("<ul>");
        for (String b : bullets) sb.append("<li>").append(safe(b)).append("</li>");
        sb.append("</ul>");
    }

    private String joinContact(ParsedResume.ContactInfo c) {
        if (c == null) return "";
        StringBuilder sb = new StringBuilder();
        appendItem(sb, c.email());
        appendItem(sb, c.phone());
        appendItem(sb, c.location());
        appendItem(sb, c.linkedin());
        appendItem(sb, c.website());
        return sb.toString();
    }

    private void appendItem(StringBuilder sb, String value) {
        if (!notBlank(value)) return;
        if (sb.length() > 0) sb.append(" • ");
        sb.append(safe(value));
    }

    private String joinDates(String start, String end) {
        boolean s = notBlank(start), e = notBlank(end);
        if (!s && !e) return "";
        if (s && e) return start + " – " + end;
        return s ? start : end;
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String safe(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }
}
