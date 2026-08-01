package com.tailored.resume.parser;

import com.tailored.resume.dto.resume.ParsedResume;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeStructureExtractorTest {

    private final ResumeStructureExtractor extractor = new ResumeStructureExtractor();

    @Test
    void extractsContactAndSections() {
        String text = """
                Jane Doe
                jane.doe@example.com | (555) 123-4567 | linkedin.com/in/janedoe
                San Francisco, CA

                Summary
                Backend engineer with 6 years building scalable services.

                Skills
                Java, Spring Boot, Kafka, PostgreSQL, AWS

                Experience
                Senior Engineer
                Acme Corp
                2021 - Present
                • Built REST APIs in Spring Boot
                • Reduced latency by 40%

                Education
                MIT
                BS Computer Science, 2018
                """;

        ParsedResume parsed = extractor.extract(text);

        assertThat(parsed.contact().email()).isEqualTo("jane.doe@example.com");
        assertThat(parsed.contact().linkedin()).contains("linkedin.com/in/janedoe");
        assertThat(parsed.summary()).contains("Backend engineer");
        assertThat(parsed.skills()).contains("Java", "Spring Boot", "Kafka");
        assertThat(parsed.experience()).hasSize(1);
        assertThat(parsed.experience().get(0).bullets()).hasSize(2);
    }
}
