package com.tailored.resume.ai;

/**
 * Helpers for handling raw model output. Models occasionally wrap JSON in
 * markdown fences despite being told not to, so strip them before parsing.
 */
public final class AiResponses {

    private AiResponses() {}

    public static String extractJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) trimmed = trimmed.substring(firstNewline + 1);
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd > 0) trimmed = trimmed.substring(0, fenceEnd);
        }
        return trimmed.strip();
    }
}
