package com.driverline.commentary.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls a local Ollama server's chat API ({@code POST /api/chat}) without streaming.
 * See https://github.com/ollama/ollama/blob/main/docs/api.md for the request and response format.
 */
public class OllamaLanguageModel implements LanguageModel {

    /** Low temperature: commentary should be factual and repeatable, not creative. */
    private static final double TEMPERATURE = 0.2;
    /** Upper bound on generated tokens, well above the ~180 words requested. */
    private static final int MAX_TOKENS = 500;

    private final RestClient restClient;
    private final OllamaProperties properties;

    /** @param restClient a client whose base URL is the Ollama server */
    public OllamaLanguageModel(RestClient restClient, OllamaProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        ChatRequest request = new ChatRequest(
                properties.model(),
                List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
                false,
                Map.of("temperature", TEMPERATURE, "num_predict", MAX_TOKENS));
        try {
            ChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.message() == null || response.message().content() == null) {
                throw new LanguageModelException("Ollama returned an empty response");
            }
            return response.message().content();
        } catch (RestClientResponseException e) {
            throw new LanguageModelException("Ollama returned HTTP %d: %s"
                    .formatted(e.getStatusCode().value(), shorten(e.getResponseBodyAsString())), e);
        } catch (ResourceAccessException e) {
            throw new LanguageModelException(isTimeout(e)
                    ? "Ollama did not answer within %d seconds".formatted(properties.timeout().toSeconds())
                    : "Could not reach Ollama at " + properties.baseUrl(), e);
        }
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static String shorten(String body) {
        String oneLine = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 200 ? oneLine.substring(0, 200) + "…" : oneLine;
    }

    record ChatRequest(String model, List<Message> messages, boolean stream, Map<String, Object> options) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }

    /** Ollama sends more fields (timings, token counts); only the message is needed. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(Message message, boolean done) {
    }
}
