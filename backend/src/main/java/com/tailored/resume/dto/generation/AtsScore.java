package com.tailored.resume.dto.generation;

import java.util.List;

public record AtsScore(
        int overall,
        int keywordMatch,
        int skillAlignment,
        int formattingQuality,
        int readability,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        /** Missing keywords with occurrence count and point value, ordered by impact. */
        List<KeywordGap> gaps,
        List<String> suggestions
) {}
