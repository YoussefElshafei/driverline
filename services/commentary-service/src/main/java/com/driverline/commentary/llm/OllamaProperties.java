package com.driverline.commentary.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from {@code commentary.ollama.*} (environment: OLLAMA_BASE_URL, OLLAMA_MODEL, OLLAMA_TIMEOUT_SECONDS). */
@ConfigurationProperties(prefix = "commentary.ollama")
public record OllamaProperties(String baseUrl, String model, Duration timeout) {
}
