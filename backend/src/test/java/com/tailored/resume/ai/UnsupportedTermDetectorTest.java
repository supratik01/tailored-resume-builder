package com.tailored.resume.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnsupportedTermDetectorTest {

    private static final String EVIDENCE = """
            {"contact":{"fullName":"Priya Narayan"},
             "summary":"Backend engineer building billing systems.",
             "skills":["Java","Spring Boot","PostgreSQL","Kafka","Docker"],
             "experience":[{"company":"Razorpe Technologies","title":"Senior Software Engineer"}]}
            """;

    @Test
    void flagsAcronymBorrowedFromJobDescription() {
        String letter = "Dear Hiring Team,\n\nI run services in Docker on AWS every day.\n\nPriya Narayan";
        assertThat(UnsupportedTermDetector.find(letter, EVIDENCE)).containsExactly("AWS");
    }

    @Test
    void flagsCapitalizedTechnologyMidSentence() {
        String letter = "I have deployed workloads with Kubernetes and Terraform at scale.";
        assertThat(UnsupportedTermDetector.find(letter, EVIDENCE))
                .containsExactly("Kubernetes", "Terraform");
    }

    @Test
    void acceptsTermsTheResumeActuallyContains() {
        String letter = """
                Dear Hiring Team,

                At Razorpe Technologies I built billing services in Java and Spring Boot, modelled
                state in PostgreSQL, and moved batch jobs onto Kafka. Docker is part of my daily work.

                Priya Narayan
                """;
        assertThat(UnsupportedTermDetector.find(letter, EVIDENCE)).isEmpty();
    }

    @Test
    void ignoresSentenceInitialCapitalizationAndSalutations() {
        String letter = """
                Dear Hiring Team,

                Seven years of backend work sit behind this application. My focus is billing.
                Please let me know the next steps.

                Priya Narayan
                """;
        assertThat(UnsupportedTermDetector.find(letter, EVIDENCE)).isEmpty();
    }

    @Test
    void reportsEachTermOnceInOrderOfAppearance() {
        String letter = "We used Terraform, then more Terraform, and later GCP.";
        assertThat(UnsupportedTermDetector.find(letter, EVIDENCE)).containsExactly("Terraform", "GCP");
    }

    @Test
    void handlesEmptyInput() {
        assertThat(UnsupportedTermDetector.find("", EVIDENCE)).isEmpty();
        assertThat(UnsupportedTermDetector.find(null, EVIDENCE)).isEmpty();
    }
}
