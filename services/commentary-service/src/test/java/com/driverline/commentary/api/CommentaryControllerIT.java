package com.driverline.commentary.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Starts the whole service with no language model available, proving it still produces commentary. */
class CommentaryControllerIT {

    /** The JSON planning-service sends: money as strings, highlights already ranked. */
    static final String REQUEST = """
            {
              "periodLabel": "FY2026, January to September",
              "revenue":   {"budget": "3223605.00", "actual": "3169643.42", "variance": "-53961.58", "variancePercent": "-1.7", "favorability": "UNFAVORABLE"},
              "expenses":  {"budget": "2936125.50", "actual": "2960379.17", "variance": "24253.67", "variancePercent": "0.8", "favorability": "UNFAVORABLE"},
              "netIncome": {"budget": "287479.50", "actual": "209264.25", "variance": "-78215.25", "variancePercent": "-27.2", "favorability": "UNFAVORABLE"},
              "highlights": [{
                "accountName": "Vehicle & Fuel", "departmentName": "Fleet & Equipment", "accountType": "EXPENSE",
                "figures": {"budget": "142065.00", "actual": "156518.61", "variance": "14453.61", "variancePercent": "10.2", "favorability": "UNFAVORABLE"}
              }]
            }
            """;

    @Nested
    @SpringBootTest(properties = "commentary.llm.provider=none")
    @AutoConfigureMockMvc
    class WithTheLanguageModelTurnedOff {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void returnsRuleBasedCommentaryAndSaysWhy() throws Exception {
            mockMvc.perform(post("/api/commentary").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provider").value("rule-based"))
                    .andExpect(jsonPath("$.model").value(nullValue()))
                    .andExpect(jsonPath("$.fallbackReason").value(containsString("turned off")))
                    .andExpect(jsonPath("$.text").value(startsWith("FY2026, January to September: net income was $209,264")))
                    .andExpect(jsonPath("$.text").value(containsString("• Vehicle & Fuel (Fleet & Equipment): $14,454 (10.2%) over budget, unfavorable.")));
        }

        @Test
        void rejectsAnInvalidRequestWithAProblemDetail() throws Exception {
            mockMvc.perform(post("/api/commentary").contentType(MediaType.APPLICATION_JSON).content("""
                            {"periodLabel": "", "highlights": []}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("periodLabel")))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("netIncome")));
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "commentary.llm.provider=ollama",
            // Port 9 (discard) has nothing listening, so the connection is refused immediately.
            "commentary.ollama.base-url=http://127.0.0.1:9"
    })
    @AutoConfigureMockMvc
    class WhenOllamaIsNotRunning {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void fallsBackToRuleBasedCommentaryInsteadOfFailing() throws Exception {
            mockMvc.perform(post("/api/commentary").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provider").value("rule-based"))
                    .andExpect(jsonPath("$.fallbackReason").value("Could not reach Ollama at http://127.0.0.1:9"))
                    .andExpect(jsonPath("$.text").value(containsString("Largest variances:")));
        }
    }
}
