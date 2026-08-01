package com.tailored.resume.ai;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds proper nouns and acronyms a generated letter uses that the candidate's own
 * resume never mentions — the signature of the model borrowing a technology from the
 * job description and attributing it to the candidate.
 *
 * <p>Deliberately conservative: it only inspects capitalized tokens and acronyms, so it
 * catches "AWS", "Kubernetes", "Terraform" and misses lowercase prose claims. Prompting
 * handles the rest; this is the backstop for the claims that matter most.
 */
public final class UnsupportedTermDetector {

    private static final Pattern TOKEN = Pattern.compile("\\b([A-Z][A-Za-z0-9+.#]{1,19})\\b");
    private static final Pattern SENTENCE_START = Pattern.compile("(?:^|[.!?]\\s+|\\n\\s*)$");

    /** Words that are capitalized for grammar, not because they name a thing. */
    private static final Set<String> IGNORED = Set.of(
            "I", "A", "An", "As", "At", "But", "By", "Dear", "Hiring", "Team", "For", "From",
            "How", "If", "In", "It", "My", "No", "Of", "On", "Or", "Please", "Sincerely",
            "So", "That", "The", "Their", "There", "They", "This", "To", "We", "What", "When",
            "While", "With", "You", "Your", "Best", "Regards", "Thank", "Thanks");

    private UnsupportedTermDetector() {}

    /**
     * @param letter   generated cover letter body
     * @param evidence everything known to be true about the candidate (resume text/JSON)
     * @return distinct terms used in the letter but absent from the evidence, in order of appearance
     */
    public static List<String> find(String letter, String evidence) {
        if (letter == null || letter.isBlank()) return List.of();
        String haystack = evidence == null ? "" : evidence.toLowerCase(Locale.ROOT);

        Set<String> unsupported = new LinkedHashSet<>();
        Matcher m = TOKEN.matcher(letter);
        while (m.find()) {
            String token = m.group(1);
            if (IGNORED.contains(token)) continue;

            boolean acronym = token.equals(token.toUpperCase(Locale.ROOT)) && token.length() >= 2;
            // A capitalized word at the start of a sentence carries no signal; an acronym does.
            if (!acronym && startsSentence(letter, m.start())) continue;

            if (!haystack.contains(token.toLowerCase(Locale.ROOT))) {
                unsupported.add(token);
            }
        }
        return List.copyOf(unsupported);
    }

    private static boolean startsSentence(String text, int index) {
        String before = text.substring(Math.max(0, index - 3), index);
        return index == 0 || SENTENCE_START.matcher(before).find();
    }
}
