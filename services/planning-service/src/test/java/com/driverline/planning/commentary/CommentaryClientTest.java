package com.driverline.planning.commentary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.commentary.dto.CommentaryRequest;
import com.driverline.planning.commentary.dto.CommentaryRequest.Figures;
import com.driverline.planning.commentary.dto.CommentaryRequest.Highlight;
import com.driverline.planning.commentary.dto.CommentaryResponse;
import com.driverline.planning.variance.Favorability;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Checks the request planning-service sends (the contract with commentary-service) and how errors are reported. */
class CommentaryClientTest {

    private static final String BASE_URL = "http://localhost:8082";

    private MockRestServiceServer server;
    private CommentaryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CommentaryClient(builder.build());
    }

    private static CommentaryRequest request() {
        Figures figures = new Figures(new BigDecimal("142065.00"), new BigDecimal("156518.61"),
                new BigDecimal("14453.61"), new BigDecimal("10.2"), Favorability.UNFAVORABLE);
        return new CommentaryRequest("FY2026, January to September", figures, figures, figures,
                List.of(new Highlight("Vehicle & Fuel", "Fleet & Equipment", AccountType.EXPENSE, figures)));
    }

    @Test
    void postsTheVariancesAndReturnsTheCommentary() {
        server.expect(requestTo(BASE_URL + "/api/commentary"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.periodLabel").value("FY2026, January to September"))
                .andExpect(jsonPath("$.netIncome.favorability").value("UNFAVORABLE"))
                .andExpect(jsonPath("$.highlights[0].accountName").value("Vehicle & Fuel"))
                .andExpect(jsonPath("$.highlights[0].departmentName").value("Fleet & Equipment"))
                .andExpect(jsonPath("$.highlights[0].accountType").value("EXPENSE"))
                .andExpect(jsonPath("$.highlights[0].figures.variance").exists())
                .andRespond(withSuccess("""
                        {"text": "Vehicle costs ran $14,454 over budget.", "provider": "ollama",
                         "model": "llama3.1:8b", "fallbackReason": null}
                        """, MediaType.APPLICATION_JSON));

        CommentaryResponse response = client.generate(request());

        assertThat(response).isEqualTo(new CommentaryResponse(
                "Vehicle costs ran $14,454 over budget.", "ollama", "llama3.1:8b", null));
        server.verify();
    }

    @Test
    void reportsThatTheCommentaryServiceIsNotRunning() {
        server.expect(requestTo(BASE_URL + "/api/commentary")).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.generate(request()))
                .isInstanceOf(CommentaryUnavailableException.class)
                .hasMessageContaining("not reachable");
    }

    @Test
    void reportsAnErrorFromTheCommentaryService() {
        server.expect(requestTo(BASE_URL + "/api/commentary")).andRespond(withServerError());

        assertThatThrownBy(() -> client.generate(request()))
                .isInstanceOf(CommentaryUnavailableException.class)
                .hasMessageContaining("HTTP 500");
    }
}
