package com.driverline.planning.commentary.dto;

/** commentary-service's answer: the text and who wrote it (a language model, or "rule-based" and why). */
public record CommentaryResponse(String text, String provider, String model, String fallbackReason) {
}
