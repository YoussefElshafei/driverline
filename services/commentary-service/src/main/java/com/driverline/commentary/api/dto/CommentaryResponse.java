package com.driverline.commentary.api.dto;

/**
 * @param provider       who wrote the text: a language model provider such as "ollama", or "rule-based"
 * @param model          the language model used, or {@code null} for rule-based text
 * @param fallbackReason why the rule-based writer was used instead of the language model, or {@code null}
 */
public record CommentaryResponse(String text, String provider, String model, String fallbackReason) {

    public static final String RULE_BASED = "rule-based";
}
