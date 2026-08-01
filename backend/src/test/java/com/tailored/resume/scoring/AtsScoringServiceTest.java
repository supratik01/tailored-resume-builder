package com.tailored.resume.scoring;

import com.tailored.resume.dto.generation.AtsScore;
import com.tailored.resume.dto.resume.ParsedResume;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtsScoringServiceTest {

    private final AtsScoringService service = new AtsScoringService(new KeywordExtractor());

    @Test
    void scoreHighlightsKeywordMatchAndMissing() {
        ParsedResume resume = new ParsedResume(
                new ParsedResume.ContactInfo("Jane Doe", "jane@example.com", "555-1234", "NYC", null, null),
                "Backend engineer with Spring Boot and Kafka experience.",
                List.of("Java", "Spring Boot", "Kafka", "PostgreSQL"),
                List.of(new ParsedResume.Experience(
                        "Acme", "Senior Engineer", "NYC", "2021", "Present",
                        List.of("Built REST APIs in Spring Boot serving 5M requests/day",
                                "Reduced p99 latency by 40% using Kafka batching"))),
                List.of(new ParsedResume.Education("MIT", "BS CS", "Computer Science", "2014", "2018", null)),
                List.of(), List.of()
        );
        String jd = "We need a Java engineer fluent in Spring Boot, Kafka, Kubernetes and AWS. PostgreSQL is a plus.";

        AtsScore score = service.score(resume, jd);

        assertThat(score.overall()).isGreaterThan(50);
        assertThat(score.matchedKeywords()).contains("spring boot", "kafka");
        assertThat(score.missingKeywords()).contains("kubernetes");
        assertThat(score.suggestions()).isNotEmpty();
    }

    @Test
    void gapListNeverNamesGenericJobPostingWords() {
        ParsedResume resume = new ParsedResume(
                new ParsedResume.ContactInfo("Jane Doe", "jane@example.com", null, null, null, null),
                "Backend engineer.",
                List.of("Java"),
                List.of(new ParsedResume.Experience("Acme", "Engineer", null, "2021", "Present",
                        List.of("Built services in Java for a payments team"))),
                List.of(), List.of(), List.of());
        String jd = """
                We are hiring a Senior Backend Engineer to run our billing platform. Experience with
                Kubernetes is essential. You will share on-call work with the team. Strong communication
                skills required. Nice to have: Terraform.
                """;

        AtsScore score = service.score(resume, jd);

        // Real technologies survive; job-posting filler does not.
        assertThat(score.missingKeywords()).contains("kubernetes", "terraform");
        assertThat(score.missingKeywords())
                .doesNotContain("run", "hir", "hiring", "share", "essential", "work", "team", "skills", "senior");
    }

    @Test
    void aSkillMentionedOnceInALongResumeIsNeverReportedMissing() {
        // Filler makes the resume long enough that a single "Docker" mention falls outside the
        // frequency cap. It is still on the page, so calling it a gap would contradict the preview.
        List<String> filler = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            filler.add("Delivered billing reconciliation improvements for merchant ledger reporting round " + i);
        }
        ParsedResume resume = new ParsedResume(
                new ParsedResume.ContactInfo("Jane Doe", "jane@example.com", null, null, null, null),
                "Backend engineer working on payments.",
                List.of("Java", "Docker"),
                List.of(new ParsedResume.Experience("Acme", "Engineer", null, "2021", "Present", filler)),
                List.of(), List.of(), List.of());

        AtsScore score = service.score(resume, "We need someone strong in Docker and Kubernetes for our billing platform.");

        assertThat(score.missingKeywords()).doesNotContain("docker");
        assertThat(score.matchedKeywords()).contains("docker");
        assertThat(score.missingKeywords()).contains("kubernetes");
    }

    @Test
    void technologyNamesEndingInSSurviveIntact() {
        ParsedResume resume = new ParsedResume(
                new ParsedResume.ContactInfo("Jane Doe", null, null, null, null, null),
                "", List.of(), List.of(), List.of(), List.of(), List.of());

        AtsScore score = service.score(resume, "You will run Kubernetes clusters backed by Redis and Postgres on AWS.");

        assertThat(score.missingKeywords()).contains("kubernetes", "redis");
        assertThat(score.missingKeywords()).doesNotContain("kubernete", "redi", "aw");
    }

    @Test
    void doesNotDoubleReportBothAPhraseAndItsLeftoverHalfWord() {
        ParsedResume resume = new ParsedResume(
                new ParsedResume.ContactInfo("Jane Doe", null, null, null, null, null),
                "", List.of(), List.of(), List.of(), List.of(), List.of());

        AtsScore score = service.score(resume, "We build event driven systems and need event driven experience.");

        assertThat(score.missingKeywords()).contains("event driven");
        assertThat(score.missingKeywords()).doesNotContain("event", "driven");
    }

    @Test
    void gapsCarryOccurrenceCountAndHonestPointValue() {
        ParsedResume resume = new ParsedResume(
                new ParsedResume.ContactInfo("Jane Doe", "jane@example.com", null, null, null, null),
                "Backend engineer.", List.of("Java"), List.of(), List.of(), List.of(), List.of());
        String jd = "Kubernetes experience required. Kubernetes clusters run our platform. Terraform is a plus.";

        AtsScore score = service.score(resume, jd);

        var kubernetes = score.gaps().stream().filter(g -> g.term().equals("kubernetes")).findFirst().orElseThrow();
        var terraform = score.gaps().stream().filter(g -> g.term().equals("terraform")).findFirst().orElseThrow();

        assertThat(kubernetes.occurrences()).isEqualTo(2);
        assertThat(terraform.occurrences()).isEqualTo(1);
        // Gaps lead with the term the posting leans on hardest.
        assertThat(score.gaps().get(0).term()).isEqualTo("kubernetes");
        // Points are a real share of the score, never zero and never more than the whole thing.
        assertThat(kubernetes.pointsIfAdded()).isBetween(1, 45);
    }

    @Test
    void keywordsNeverKeepTrailingPunctuation() {
        ParsedResume resume = new ParsedResume(
                new ParsedResume.ContactInfo("Jane Doe", null, null, null, null, null),
                "", List.of(), List.of(), List.of(), List.of(), List.of());

        AtsScore score = service.score(resume, "The platform runs on Terraform. Redis. And Kafka.");

        assertThat(score.missingKeywords()).noneMatch(k -> k.endsWith("."));
    }

    @Test
    void emptyResumeProducesLowScores() {
        ParsedResume empty = new ParsedResume(
                new ParsedResume.ContactInfo("", null, null, null, null, null),
                "", List.of(), List.of(), List.of(), List.of(), List.of());
        AtsScore score = service.score(empty, "Spring Boot Java Kubernetes engineer required.");
        assertThat(score.overall()).isLessThan(40);
        assertThat(score.keywordMatch()).isEqualTo(0);
    }
}
