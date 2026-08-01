package com.tailored.resume.scoring;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Lightweight keyword extractor: tokenize, lower-case, strip punctuation, drop stop-words,
 * normalize simple plural forms, return frequency-sorted unigrams plus a hand-curated list
 * of multi-word tech phrases (e.g. "machine learning", "spring boot").
 *
 * <p>Two rules exist because these terms are shown to the user as "add this to your resume":
 * tokens keep their real shape (no aggressive stemming that turns "hiring" into "hir"), and
 * {@link #isReportable(String)} filters generic English out of anything user-facing.
 */
@Component
public class KeywordExtractor {

    /** Must start and end on a word character, so trailing punctuation never survives. */
    private static final Pattern TOKEN = Pattern.compile("[a-z][a-z0-9+#./]*[a-z0-9+#]");

    private static final Set<String> STOPWORDS = Set.of(
            "the","a","an","and","or","but","of","in","on","at","to","for","with","by","from","is","are","was","were",
            "be","been","being","this","that","these","those","it","its","as","we","you","your","our","i","my","me",
            "they","them","their","he","she","his","her","will","shall","can","may","must","should","would","could",
            "do","does","did","done","have","has","had","not","no","yes","if","then","than","so","too","very","also",
            "into","onto","over","under","about","across","through","up","down","out","off","just","like","such","more",
            "most","some","any","all","each","every","other","another","new","old","good","great","best","one","two",
            "three","year","years","month","months","day","days","week","weeks","candidate","candidates","you'll","we're","etc"
    );

    /**
     * Words that are common in job postings but are not skills. Surfacing these as gaps
     * ("add: essential") destroys trust in the whole score, so they never reach the user.
     */
    private static final Set<String> NOT_A_SKILL = Set.of(
            "ability","able","apply","applicant","applicants","background","benefits","bonus","build","building",
            "built","company","competitive","culture","deliver","description","design","desirable","drive","essential",
            "excellent","experience","experienced","familiar","familiarity","fast","flexible","focus","growth","help",
            "hire","hiring","hybrid","impact","include","included","includes","including","job","join","knowledge",
            "language","large","lead","leading","learn","learning","level","looking","love","maintain","make","mission",
            "must-have","need","needs","nice","office","opportunity","own","ownership","paced","part","partner","pay",
            "people","plus","position","preferred","product","products","proven","provide","qualification",
            "qualifications","quality","remote","required","requirement","requirements","responsibilities",
            "responsibility","role","roles","run","running","salary","scale","senior","share","skill","skills","solve",
            "stack","staff","strong","support","team","teams","technical","technology","tool","tools","understand",
            "understanding","use","using","values","want","work","working","world","write","writing","hands-on",
            "collaborate","collaboration","communication","environment","engineer","engineering","engineers","develop",
            "developer","developers","development","solutions","business","customer","customers","client","clients",
            "day-to-day","end-to-end","full-time","junior","mid","related","relevant","similar","successful","talented",
            "top","expertise","exposure","passion","passionate","curious","comfortable","confident",
            // Generic nouns that read as technologies but tell the user nothing actionable.
            "platform","platforms","model","models","flow","flows","state","system","systems","service",
            "services","backend","frontend","full-stack","fullstack","stacks","project","projects","process",
            "processes","feature","features","code","codebase","app","apps","application","applications",
            "mentoring","mentor","mentorship","onboarding","standard","standards","practice","practices",
            "approach","area","areas","case","cases","issue","issues","problem","problems","task","tasks",
            "time","timeline","delivery","deliverable","deliverables","stakeholder","stakeholders",
            "partners","user","users","member","members","department","initiative","initiatives",
            // Halves of curated multi-word phrases (KNOWN_PHRASES already captures the signal;
            // the leftover single word is noise — "add: driven" is not actionable advice).
            "driven","event","oriented","object","functional"
    );

    /**
     * Technology names that legitimately end in "s". Without this, plural stripping turns
     * "kubernetes" into "kubernete" and "redis" into "redi", and both then show up as gaps.
     */
    private static final Set<String> NEVER_STEM = Set.of(
            "kubernetes","redis","aws","devops","mlops","analytics","kibana","jenkins","elasticsearch",
            "postgres","https","js","nodejs","rails","docs","ops","ios","css","sass","less","dns","cors",
            "graphs","statistics","mathematics","physics","sales","operations","logistics"
    );

    private static final List<String> KNOWN_PHRASES = List.of(
            "machine learning", "deep learning", "data science", "data engineering", "data analysis",
            "natural language processing", "computer vision", "large language models",
            "spring boot", "spring security", "spring data", "spring cloud",
            "node.js", "next.js", "react native", "type script",
            "ci/cd", "continuous integration", "continuous deployment", "test driven development",
            "object oriented", "functional programming", "domain driven design",
            "rest api", "rest apis", "graphql api", "micro services", "microservices architecture",
            "event driven", "message queue", "load balancer", "load balancing",
            "unit testing", "integration testing", "end to end testing",
            "agile", "scrum", "kanban", "code review", "pair programming",
            "amazon web services", "google cloud platform", "microsoft azure",
            "kubernetes", "docker compose", "helm chart", "service mesh",
            "single page application", "server side rendering",
            "version control", "system design", "distributed systems"
    );

    /** Top terms by frequency. Use for the job description, where emphasis is the signal. */
    public Set<String> extract(String text) {
        return extract(text, 80);
    }

    /**
     * Every term, uncapped. Use for the resume side: a skill listed once still counts as
     * present, and capping there reports things the resume plainly says as missing.
     */
    public Set<String> extractAll(String text) {
        return extract(text, Integer.MAX_VALUE);
    }

    private Set<String> extract(String text, int limit) {
        if (text == null || text.isBlank()) return Set.of();
        String lower = text.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String phrase : KNOWN_PHRASES) {
            if (lower.contains(phrase)) result.add(phrase);
        }
        var matcher = TOKEN.matcher(lower);
        Map<String, Integer> counts = new HashMap<>();
        while (matcher.find()) {
            String tok = normalize(matcher.group());
            if (tok.length() < 2 || STOPWORDS.contains(tok)) continue;
            counts.merge(tok, 1, Integer::sum);
        }
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> result.add(e.getKey()));
        return result;
    }

    /**
     * Whether a keyword is worth showing the user as a gap to close. Scoring still counts
     * every token; this only governs what gets named on screen.
     */
    public boolean isReportable(String keyword) {
        if (keyword == null) return false;
        String k = keyword.strip();
        if (k.contains(" ")) return true;               // curated phrases are always meaningful
        if (k.length() < 3) return false;               // "ai" and "go" lose to noise at this length
        if (STOPWORDS.contains(k) || NOT_A_SKILL.contains(k)) return false;
        return !k.endsWith(".");
    }

    /** Keeps only the keywords worth naming to a user, in their original order. */
    public List<String> reportable(Collection<String> keywords) {
        return keywords.stream().filter(this::isReportable).toList();
    }

    /**
     * Conservative plural handling only. Verb stemming was removed deliberately: it produced
     * "hir" from "hiring" and "runn" from "running", which then appeared in the user's gap list.
     */
    private String normalize(String token) {
        if (NEVER_STEM.contains(token)) return token;
        if (token.endsWith("ies") && token.length() > 4) return token.substring(0, token.length() - 3) + "y";
        if (token.endsWith("sses") && token.length() > 5) return token.substring(0, token.length() - 2);
        if (token.endsWith("s") && !token.endsWith("ss") && !token.endsWith("us") && token.length() > 3) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
