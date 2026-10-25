package com.driverline.planning.commentary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.driverline.planning.TestcontainersConfiguration;
import com.driverline.planning.commentary.dto.CommentaryRequest;
import com.driverline.planning.commentary.dto.CommentaryResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The real variance report from the seeded Ace Janitorial data, with only the HTTP call to commentary-service
 * replaced by a mock, so the test can check exactly what planning-service would send.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CommentaryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentaryClient commentaryClient;

    @Test
    void sendsTheLargestRealVariancesAndReturnsTheCommentary() throws Exception {
        when(commentaryClient.generate(any())).thenReturn(new CommentaryResponse(
                "Contract revenue was the largest shortfall.", "ollama", "llama3.1:8b", null));

        mockMvc.perform(get("/api/variance/commentary").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodLabel").value("FY2026, January to September"))
                .andExpect(jsonPath("$.fromMonth").value(1))
                .andExpect(jsonPath("$.toMonth").value(9))
                .andExpect(jsonPath("$.text").value("Contract revenue was the largest shortfall."))
                .andExpect(jsonPath("$.provider").value("ollama"))
                .andExpect(jsonPath("$.model").value("llama3.1:8b"));

        ArgumentCaptor<CommentaryRequest> sent = ArgumentCaptor.forClass(CommentaryRequest.class);
        verify(commentaryClient).generate(sent.capture());
        assertThat(sent.getValue().highlights()).hasSize(VarianceHighlightSelector.DEFAULT_LIMIT);
        // The lost office-tower contract is the demo company's largest FY2026 variance.
        assertThat(sent.getValue().highlights().getFirst().accountName()).isEqualTo("Contract Cleaning Revenue");
        assertThat(sent.getValue().netIncome().variance()).isNegative();
    }

    @Test
    void passesThroughRuleBasedCommentaryAndItsReason() throws Exception {
        when(commentaryClient.generate(any())).thenReturn(new CommentaryResponse(
                "FY2025: net income was ...", "rule-based", null, "Could not reach Ollama at http://localhost:11434"));

        mockMvc.perform(get("/api/variance/commentary").param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("rule-based"))
                .andExpect(jsonPath("$.fallbackReason").value("Could not reach Ollama at http://localhost:11434"));
    }

    @Test
    void returns503WhenTheCommentaryServiceIsDown() throws Exception {
        when(commentaryClient.generate(any())).thenThrow(new CommentaryUnavailableException(
                "The commentary service is not reachable. Start commentary-service and try again.", null));

        mockMvc.perform(get("/api/variance/commentary").param("year", "2026"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Commentary unavailable"))
                .andExpect(jsonPath("$.detail").value("The commentary service is not reachable. Start commentary-service and try again."));
    }

    @Test
    void validatesTheMonthRange() throws Exception {
        mockMvc.perform(get("/api/variance/commentary").param("year", "2026").param("fromMonth", "10").param("toMonth", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("toMonth"));
    }
}
