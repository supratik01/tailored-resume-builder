package com.tailored.resume.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tailored.resume.dto.resume.ParsedResume;
import com.tailored.resume.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PdfExporter {

    private final ResumeHtmlRenderer renderer;

    public byte[] export(ParsedResume resume) {
        String html = renderer.render(resume);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to render PDF: " + e.getMessage());
        }
    }
}
