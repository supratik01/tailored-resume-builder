package com.tailored.resume.parser;

import com.tailored.resume.entity.Resume;
import com.tailored.resume.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TextExtractor {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    public Extracted extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File exceeds 10MB limit");
        }
        String filename = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        String lower = filename.toLowerCase();
        try (InputStream in = file.getInputStream()) {
            if (lower.endsWith(".pdf")) {
                return new Extracted(extractPdf(in), Resume.FileType.PDF, filename);
            } else if (lower.endsWith(".docx")) {
                return new Extracted(extractDocx(in), Resume.FileType.DOCX, filename);
            } else {
                throw new BadRequestException("Only PDF and DOCX files are supported");
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file: " + e.getMessage());
        }
    }

    private String extractPdf(InputStream in) throws IOException {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return normalize(stripper.getText(doc));
        }
    }

    private String extractDocx(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append('\n');
            }
            doc.getTables().forEach(t -> t.getRows().forEach(r -> r.getTableCells().forEach(c -> {
                sb.append(c.getText()).append('\n');
            })));
        }
        return normalize(sb.toString());
    }

    private String normalize(String s) {
        return s.replace("\r\n", "\n").replace(' ', ' ').trim();
    }

    public record Extracted(String text, Resume.FileType type, String originalFilename) {}
}
