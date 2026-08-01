package com.tailored.resume.scoring;

import com.tailored.resume.dto.generation.AtsScore;
import com.tailored.resume.dto.generation.KeywordGap;
import com.tailored.resume.dto.resume.ParsedResume;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AtsScoringService {

    private static final int TOP_MISSING = 10;
    private static final int TOP_MATCHED = 15;

    /** Keyword match's share of the overall score. Kept as a constant so the points shown
     *  next to each gap can never drift from the formula that produced the score. */
    private static final double KEYWORD_WEIGHT = 0.45;

    private final KeywordExtractor extractor;

    public AtsScore score(ParsedResume tailored, String jobDescription) {
        Set<String> jdKeywords = extractor.extract(jobDescription);
        String resumeText = flattenResume(tailored);
        Set<String> resumeKeywords = extractor.extractAll(resumeText);

        LinkedHashSet<String> matched = new LinkedHashSet<>(jdKeywords);
        matched.retainAll(resumeKeywords);
        LinkedHashSet<String> missing = new LinkedHashSet<>(jdKeywords);
        missing.removeAll(resumeKeywords);

        int keywordMatch = jdKeywords.isEmpty() ? 0 : (int) Math.round(100.0 * matched.size() / jdKeywords.size());
        int skillAlignment = skillAlignment(tailored.skills(), jdKeywords);
        int formattingQuality = formattingQuality(tailored);
        int readability = readability(tailored);
        int overall = (int) Math.round(KEYWORD_WEIGHT * keywordMatch + 0.25 * skillAlignment
                + 0.15 * formattingQuality + 0.15 * readability);

        // Scoring counts every token; the user only ever sees keywords worth acting on.
        List<String> reportableMissing = extractor.reportable(missing).stream().limit(TOP_MISSING).toList();
        List<String> reportableMatched = extractor.reportable(matched).stream().limit(TOP_MATCHED).toList();

        List<KeywordGap> gaps = gaps(reportableMissing, jobDescription, jdKeywords.size());
        List<String> suggestions = suggestions(tailored, reportableMissing, formattingQuality);

        return new AtsScore(
                overall,
                keywordMatch,
                skillAlignment,
                formattingQuality,
                readability,
                reportableMatched,
                reportableMissing,
                gaps,
                suggestions
        );
    }

    /**
     * Turns bare missing terms into rows a user can act on. Points come from the actual
     * scoring weights: keyword match is 45% of overall, shared across the JD's terms, so
     * covering one term is worth 0.45 * (100 / termCount) points. Ordered by how hard the
     * posting leans on the term.
     */
    private List<KeywordGap> gaps(List<String> missing, String jobDescription, int jdKeywordCount) {
        if (missing.isEmpty() || jdKeywordCount == 0) return List.of();
        String jd = jobDescription == null ? "" : jobDescription.toLowerCase();
        int points = Math.max(1, (int) Math.round(KEYWORD_WEIGHT * 100.0 / jdKeywordCount));

        return missing.stream()
                .map(term -> new KeywordGap(term, countOccurrences(jd, term), points))
                .sorted(Comparator.comparingInt(KeywordGap::occurrences).reversed())
                .toList();
    }

    private int countOccurrences(String haystack, String needle) {
        if (needle == null || needle.isBlank()) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int i = haystack.indexOf(needle, from);
            if (i < 0) break;
            count++;
            from = i + needle.length();
        }
        return Math.max(1, count);
    }

    private String flattenResume(ParsedResume r) {
        StringBuilder sb = new StringBuilder();
        if (r.summary() != null) sb.append(r.summary()).append(' ');
        if (r.skills() != null) sb.append(String.join(" ", r.skills())).append(' ');
        if (r.experience() != null) {
            for (var e : r.experience()) {
                sb.append(safe(e.title())).append(' ').append(safe(e.company())).append(' ');
                if (e.bullets() != null) sb.append(String.join(" ", e.bullets())).append(' ');
            }
        }
        if (r.projects() != null) {
            for (var p : r.projects()) {
                sb.append(safe(p.name())).append(' ').append(safe(p.description())).append(' ');
                if (p.bullets() != null) sb.append(String.join(" ", p.bullets())).append(' ');
                if (p.tech() != null) sb.append(String.join(" ", p.tech())).append(' ');
            }
        }
        if (r.education() != null) {
            for (var ed : r.education()) {
                sb.append(safe(ed.institution())).append(' ').append(safe(ed.degree())).append(' ').append(safe(ed.field())).append(' ');
            }
        }
        if (r.certifications() != null) sb.append(String.join(" ", r.certifications()));
        return sb.toString();
    }

    private int skillAlignment(List<String> skills, Set<String> jdKeywords) {
        if (skills == null || skills.isEmpty()) return 0;
        Set<String> jdLower = jdKeywords;
        long hits = skills.stream()
                .map(s -> s.toLowerCase().strip())
                .filter(s -> jdLower.stream().anyMatch(k -> k.contains(s) || s.contains(k)))
                .count();
        return (int) Math.round(100.0 * hits / Math.max(1, skills.size()));
    }

    private int formattingQuality(ParsedResume r) {
        int score = 100;
        if (r.summary() == null || r.summary().isBlank()) score -= 15;
        if (r.skills() == null || r.skills().isEmpty()) score -= 15;
        if (r.experience() == null || r.experience().isEmpty()) score -= 30;
        if (r.education() == null || r.education().isEmpty()) score -= 10;
        if (r.contact() != null && (r.contact().email() == null || r.contact().email().isBlank())) score -= 10;
        return Math.max(0, score);
    }

    private int readability(ParsedResume r) {
        if (r.experience() == null || r.experience().isEmpty()) return 50;
        int totalBullets = 0;
        int wellSized = 0;
        for (var e : r.experience()) {
            if (e.bullets() == null) continue;
            for (String b : e.bullets()) {
                totalBullets++;
                int words = b.split("\\s+").length;
                if (words >= 8 && words <= 28) wellSized++;
            }
        }
        if (totalBullets == 0) return 50;
        return (int) Math.round(100.0 * wellSized / totalBullets);
    }

    private List<String> suggestions(ParsedResume r, List<String> missing, int formattingQuality) {
        List<String> out = new ArrayList<>();
        // The missing terms are already listed on screen; suggestions cover what that list can't say.
        if (r.summary() == null || r.summary().isBlank()) {
            out.add("Add a 2–4 sentence professional summary targeting the role.");
        }
        if (r.experience() != null) {
            boolean anyQuantified = r.experience().stream()
                    .flatMap(e -> e.bullets() == null ? java.util.stream.Stream.empty() : e.bullets().stream())
                    .anyMatch(b -> b.matches(".*\\d.*"));
            if (!anyQuantified) out.add("Quantify at least 2–3 bullets (numbers, %, $, time saved) — recruiters scan for impact.");
        }
        if (formattingQuality < 80) {
            out.add("Tighten formatting: ensure summary, skills, experience, and contact email are all present.");
        }
        if (r.skills() != null && r.skills().size() < 8) {
            out.add("Expand the skills section to 10–15 items, weighted toward JD-relevant technologies.");
        }
        if (out.isEmpty() && !missing.isEmpty()) {
            out.add("Work the terms above into the bullets where they are true — a skills-list mention counts for less.");
        }
        return out;
    }

    private String safe(String s) { return s == null ? "" : s; }
}
