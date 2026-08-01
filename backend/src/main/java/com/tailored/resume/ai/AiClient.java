package com.tailored.resume.ai;

/**
 * Pluggable LLM client. Implementations: {@link OpenAiClient} for production,
 * stub doubles for tests. Keeping this thin lets us swap models or providers
 * (Anthropic, Bedrock, local) without touching the tailoring service.
 */
public interface AiClient {

    /**
     * @param systemPrompt instructions that set tone / hard rules
     * @param userPrompt   the task content
     * @return raw model output (the caller decides how to parse — JSON, plaintext, etc.)
     */
    String complete(String systemPrompt, String userPrompt);
}
