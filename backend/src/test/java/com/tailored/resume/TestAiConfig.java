package com.tailored.resume;

import com.tailored.resume.ai.AiClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestAiConfig {

    @Bean
    @Primary
    AiClient stubAiClient() {
        return (system, user) -> """
                {
                  "contact": { "fullName": "Test User", "email": "t@example.com", "phone": null, "location": null, "linkedin": null, "website": null },
                  "summary": "Test summary.",
                  "skills": ["Java", "Spring Boot"],
                  "experience": [],
                  "education": [],
                  "projects": [],
                  "certifications": []
                }
                """;
    }
}
