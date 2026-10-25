package com.driverline.commentary.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Uses Spring's MockRestServiceServer in place of a real Ollama server, so no LLM is needed. */
class OllamaLanguageModelTest {

    private static final String BASE_URL = "http://localhost:11434";
    private static final OllamaProperties PROPERTIES =
            new OllamaProperties(BASE_URL, "llama3.1:8b", Duration.ofSeconds(120));

    private MockRestServiceServer server;
    private OllamaLanguageModel model;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        model = new OllamaLanguageModel(builder.build(), PROPERTIES);
    }

    @Test
    void sendsANonStreamingChatRequestAndReturnsTheMessageContent() {
        server.expect(requestTo(BASE_URL + "/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("llama3.1:8b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("Be precise."))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.options.temperature").value(0.2))
                .andRespond(withSuccess("""
                        {"model": "llama3.1:8b", "created_at": "2026-10-25T10:00:00Z",
                         "message": {"role": "assistant", "content": "Net income was $209,264."},
                         "done": true, "total_duration": 1234567, "eval_count": 9}
                        """, MediaType.APPLICATION_JSON));

        String text = model.generate("Be precise.", "Data...");

        assertThat(text).isEqualTo("Net income was $209,264.");
        server.verify();
    }

    @Test
    void reportsAnErrorResponseFromOllamaClearly() {
        server.expect(requestTo(BASE_URL + "/api/chat"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"model 'llama3.1:8b' not found\"}"));

        assertThatThrownBy(() -> model.generate("system", "user"))
                .isInstanceOf(LanguageModelException.class)
                .hasMessageContaining("HTTP 404")
                .hasMessageContaining("not found");
    }

    @Test
    void reportsThatOllamaCouldNotBeReached() {
        server.expect(requestTo(BASE_URL + "/api/chat")).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> model.generate("system", "user"))
                .isInstanceOf(LanguageModelException.class)
                .hasMessage("Could not reach Ollama at http://localhost:11434");
    }

    @Test
    void reportsATimeoutWithTheConfiguredLimit() {
        server.expect(requestTo(BASE_URL + "/api/chat")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> model.generate("system", "user"))
                .isInstanceOf(LanguageModelException.class)
                .hasMessage("Ollama did not answer within 120 seconds");
    }

    @Test
    void treatsAResponseWithoutAMessageAsAnError() {
        server.expect(requestTo(BASE_URL + "/api/chat")).andRespond(withSuccess("{\"done\": true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> model.generate("system", "user"))
                .isInstanceOf(LanguageModelException.class)
                .hasMessage("Ollama returned an empty response");
    }
}
