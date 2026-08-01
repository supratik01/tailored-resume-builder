package com.tailored.resume.dto.generation;

/**
 * One missing keyword, with the two facts that make it actionable: how hard the posting
 * leans on it, and what closing it is actually worth.
 *
 * @param term          the keyword, exactly as it reads in the posting
 * @param occurrences   how many times it appears in the job description
 * @param pointsIfAdded overall-score points this term is worth, derived from the keyword-match
 *                      weight. Honest arithmetic, not an estimate: keyword match is 45% of the
 *                      overall score, spread evenly across the terms the posting emphasises.
 */
public record KeywordGap(
        String term,
        int occurrences,
        int pointsIfAdded
) {}
