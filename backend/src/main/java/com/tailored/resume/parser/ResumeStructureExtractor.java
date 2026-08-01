package com.tailored.resume.parser;

import com.tailored.resume.dto.resume.ParsedResume;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort heuristic resume sectioning. Imperfect by design: the AI pass downstream
 * cleans up anything we get wrong. We just need plausible structure for the prompt.
 */
@Component
public class ResumeStructureExtractor {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d{1,3}[\\s-]?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}");
    private static final Pattern LINKEDIN = Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[\\w-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL = Pattern.compile("(?:https?://)?(?:www\\.)?[\\w.-]+\\.(?:com|io|dev|me|net|org|co)(?:/[\\w./?=&-]*)?", Pattern.CASE_INSENSITIVE);

    private static final Map<String, List<String>> SECTION_ALIASES = new LinkedHashMap<>();
    static {
        SECTION_ALIASES.put("summary", List.of("summary", "professional summary", "profile", "objective", "about"));
        SECTION_ALIASES.put("skills", List.of("skills", "technical skills", "core competencies", "technologies"));
        SECTION_ALIASES.put("experience", List.of("experience", "work experience", "professional experience", "employment"));
        SECTION_ALIASES.put("education", List.of("education", "academic background", "academics"));
        SECTION_ALIASES.put("projects", List.of("projects", "personal projects", "selected projects"));
        SECTION_ALIASES.put("certifications", List.of("certifications", "licenses", "certificates"));
    }

    public ParsedResume extract(String rawText) {
        List<String> lines = Arrays.stream(rawText.split("\n"))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .toList();

        ParsedResume.ContactInfo contact = parseContact(lines);
        Map<String, List<String>> sections = splitSections(lines);

        return new ParsedResume(
                contact,
                joinSection(sections.get("summary")),
                parseSkills(sections.get("skills")),
                parseExperience(sections.get("experience")),
                parseEducation(sections.get("education")),
                parseProjects(sections.get("projects")),
                parseList(sections.get("certifications"))
        );
    }

    private ParsedResume.ContactInfo parseContact(List<String> lines) {
        String name = lines.isEmpty() ? "" : lines.get(0);
        if (looksLikeContactLine(name)) name = "";

        String full = String.join(" ", lines.subList(0, Math.min(6, lines.size())));
        String email = firstMatch(EMAIL, full);
        String phone = firstMatch(PHONE, full);
        String linkedin = firstMatch(LINKEDIN, full);
        String website = null;
        Matcher m = URL.matcher(full);
        while (m.find()) {
            String found = m.group();
            if (linkedin == null || !found.toLowerCase().contains("linkedin")) {
                website = found;
                if (linkedin != null && !found.equalsIgnoreCase(linkedin)) break;
            }
        }
        String location = lines.stream().skip(1).limit(5)
                .filter(l -> l.matches(".*[A-Za-z],\\s?[A-Za-z]{2,}.*") && !l.contains("@"))
                .findFirst().orElse(null);

        return new ParsedResume.ContactInfo(name, email, phone, location, linkedin, website);
    }

    private boolean looksLikeContactLine(String s) {
        return EMAIL.matcher(s).find() || PHONE.matcher(s).find() || s.contains("linkedin.com");
    }

    private Map<String, List<String>> splitSections(List<String> lines) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String key : SECTION_ALIASES.keySet()) result.put(key, new ArrayList<>());

        String current = null;
        for (String line : lines) {
            String headingKey = matchHeading(line);
            if (headingKey != null) {
                current = headingKey;
                continue;
            }
            if (current != null) {
                result.get(current).add(line);
            }
        }
        return result;
    }

    private String matchHeading(String line) {
        if (line.length() > 60) return null;
        String norm = line.toLowerCase().replaceAll("[^a-z ]", "").strip();
        for (var entry : SECTION_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (norm.equals(alias)) return entry.getKey();
            }
        }
        return null;
    }

    private String joinSection(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        return String.join(" ", lines).trim();
    }

    private List<String> parseSkills(List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            for (String tok : line.split("[,;|•·\\u2022\\u25CF]")) {
                String s = tok.replaceAll("^[-•·\\s]+", "").strip();
                if (!s.isBlank() && s.length() <= 60) result.add(s);
            }
        }
        return result.stream().distinct().toList();
    }

    private List<ParsedResume.Experience> parseExperience(List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<ParsedResume.Experience> result = new ArrayList<>();
        List<String> currentHeader = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("·")) {
                bullets.add(line.replaceAll("^[-•·*\\s]+", "").strip());
            } else {
                if (!currentHeader.isEmpty() && !bullets.isEmpty()) {
                    result.add(toExperience(currentHeader, bullets));
                    currentHeader = new ArrayList<>();
                    bullets = new ArrayList<>();
                }
                currentHeader.add(line);
            }
        }
        if (!currentHeader.isEmpty()) {
            result.add(toExperience(currentHeader, bullets));
        }
        return result;
    }

    private ParsedResume.Experience toExperience(List<String> header, List<String> bullets) {
        String title = header.isEmpty() ? "" : header.get(0);
        String company = header.size() > 1 ? header.get(1) : "";
        String dates = header.size() > 2 ? header.get(2) : "";
        return new ParsedResume.Experience(company, title, "", dates, "", List.copyOf(bullets));
    }

    private List<ParsedResume.Education> parseEducation(List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<ParsedResume.Education> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += 2) {
            String first = lines.get(i);
            String second = i + 1 < lines.size() ? lines.get(i + 1) : "";
            result.add(new ParsedResume.Education(first, second, "", "", "", ""));
        }
        return result;
    }

    private List<ParsedResume.Project> parseProjects(List<String> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        List<ParsedResume.Project> result = new ArrayList<>();
        String currentName = null;
        List<String> bullets = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("·")) {
                bullets.add(line.replaceAll("^[-•·*\\s]+", "").strip());
            } else {
                if (currentName != null) {
                    result.add(new ParsedResume.Project(currentName, "", List.copyOf(bullets), List.of()));
                    bullets = new ArrayList<>();
                }
                currentName = line;
            }
        }
        if (currentName != null) {
            result.add(new ParsedResume.Project(currentName, "", List.copyOf(bullets), List.of()));
        }
        return result;
    }

    private List<String> parseList(List<String> lines) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        return lines.stream()
                .map(l -> l.replaceAll("^[-•·*\\s]+", "").strip())
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String firstMatch(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group() : null;
    }
}
