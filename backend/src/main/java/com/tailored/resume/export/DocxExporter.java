package com.tailored.resume.export;

import com.tailored.resume.dto.resume.ParsedResume;
import com.tailored.resume.exception.BadRequestException;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Component
public class DocxExporter {

    public byte[] export(ParsedResume r) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            heading(doc, r.contact() == null ? "" : safe(r.contact().fullName()), 22, true, ParagraphAlignment.CENTER);
            if (r.contact() != null) {
                paragraph(doc, contactLine(r.contact()), 10, false, ParagraphAlignment.CENTER, false);
            }

            if (notBlank(r.summary())) {
                sectionHeading(doc, "SUMMARY");
                paragraph(doc, r.summary(), 11, false, ParagraphAlignment.LEFT, false);
            }
            if (r.skills() != null && !r.skills().isEmpty()) {
                sectionHeading(doc, "SKILLS");
                paragraph(doc, String.join(" • ", r.skills()), 11, false, ParagraphAlignment.LEFT, false);
            }
            if (r.experience() != null && !r.experience().isEmpty()) {
                sectionHeading(doc, "EXPERIENCE");
                for (var e : r.experience()) {
                    String header = safe(e.title());
                    if (notBlank(e.company())) header += " — " + e.company();
                    String dates = joinDates(e.startDate(), e.endDate());
                    roleHeader(doc, header, dates);
                    writeBullets(doc, e.bullets());
                }
            }
            if (r.projects() != null && !r.projects().isEmpty()) {
                sectionHeading(doc, "PROJECTS");
                for (var p : r.projects()) {
                    String tech = (p.tech() == null || p.tech().isEmpty()) ? "" : " — " + String.join(", ", p.tech());
                    roleHeader(doc, safe(p.name()) + tech, "");
                    if (notBlank(p.description())) paragraph(doc, p.description(), 11, false, ParagraphAlignment.LEFT, false);
                    writeBullets(doc, p.bullets());
                }
            }
            if (r.education() != null && !r.education().isEmpty()) {
                sectionHeading(doc, "EDUCATION");
                for (var ed : r.education()) {
                    String header = safe(ed.institution());
                    if (notBlank(ed.degree())) header += " — " + ed.degree();
                    if (notBlank(ed.field())) header += ", " + ed.field();
                    roleHeader(doc, header, joinDates(ed.startDate(), ed.endDate()));
                    if (notBlank(ed.details())) paragraph(doc, ed.details(), 11, false, ParagraphAlignment.LEFT, false);
                }
            }
            if (r.certifications() != null && !r.certifications().isEmpty()) {
                sectionHeading(doc, "CERTIFICATIONS");
                writeBullets(doc, r.certifications());
            }

            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to render DOCX: " + e.getMessage());
        }
    }

    private void heading(XWPFDocument doc, String text, int size, boolean bold, ParagraphAlignment align) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(size);
        run.setFontFamily("Helvetica");
    }

    private void sectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        p.setSpacingAfter(40);
        addBottomBorder(p);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(11);
        run.setFontFamily("Helvetica");
    }

    private void addBottomBorder(XWPFParagraph p) {
        CTPPr ppr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        CTPBdr borders = ppr.isSetPBdr() ? ppr.getPBdr() : ppr.addNewPBdr();
        CTBorder bottom = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setSz(BigInteger.valueOf(6));
        bottom.setColor("000000");
    }

    private void paragraph(XWPFDocument doc, String text, int size, boolean bold, ParagraphAlignment align, boolean italic) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setItalic(italic);
        run.setFontSize(size);
        run.setFontFamily("Helvetica");
    }

    private void roleHeader(XWPFDocument doc, String left, String right) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(60);
        XWPFRun lr = p.createRun();
        lr.setText(left);
        lr.setBold(true);
        lr.setFontSize(11);
        lr.setFontFamily("Helvetica");
        if (notBlank(right)) {
            XWPFRun rr = p.createRun();
            rr.setText("\t" + right);
            rr.setFontSize(10);
            rr.setFontFamily("Helvetica");
            rr.setColor("444444");
        }
    }

    private void writeBullets(XWPFDocument doc, List<String> bullets) {
        if (bullets == null) return;
        for (String b : bullets) {
            if (b == null || b.isBlank()) continue;
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationLeft(360);
            XWPFRun run = p.createRun();
            run.setText("• " + b);
            run.setFontSize(11);
            run.setFontFamily("Helvetica");
        }
    }

    private String contactLine(ParsedResume.ContactInfo c) {
        StringBuilder sb = new StringBuilder();
        appendItem(sb, c.email());
        appendItem(sb, c.phone());
        appendItem(sb, c.location());
        appendItem(sb, c.linkedin());
        appendItem(sb, c.website());
        return sb.toString();
    }

    private void appendItem(StringBuilder sb, String v) {
        if (!notBlank(v)) return;
        if (sb.length() > 0) sb.append(" • ");
        sb.append(v);
    }

    private String joinDates(String start, String end) {
        boolean s = notBlank(start), e = notBlank(end);
        if (!s && !e) return "";
        if (s && e) return start + " – " + end;
        return s ? start : end;
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String safe(String s) { return s == null ? "" : s; }
}
