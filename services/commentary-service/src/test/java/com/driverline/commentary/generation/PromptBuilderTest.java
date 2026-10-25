package com.driverline.commentary.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.commentary.SampleRequests;
import com.driverline.commentary.generation.PromptBuilder.Prompt;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    private final Prompt prompt = new PromptBuilder().build(SampleRequests.aceJanitorialFy2026());

    @Test
    void systemPromptTellsTheModelToUseOnlyTheGivenFiguresAndNotInventCauses() {
        assertThat(prompt.system())
                .contains("Use only the figures in the data")
                .contains("Copy every dollar amount and percentage exactly")
                .contains("Do not invent reasons")
                .contains("under 180 words");
    }

    @Test
    void userPromptListsTheTotalsAndRankedHighlightsWithPreformattedFigures() {
        assertThat(prompt.user())
                .contains("Period: FY2026, January to September")
                .contains("- Net income: actual $209,264; budget $287,480; $78,215 (27.2%) below budget, unfavorable")
                .contains("1. Contract Cleaning Revenue (Sales & Account Management, revenue account): "
                        + "actual $2,751,086; budget $2,795,145; $44,059 (1.6%) below budget, unfavorable")
                .contains("4. Wages - Cleaning Staff (Operations, expense account)")
                .endsWith("Write the commentary now.");
    }

    @Test
    void spellsOutTheDirectionInWordsInsteadOfRelyingOnSigns() {
        assertThat(prompt.user())
                .contains("- Revenue: actual $3,169,643; budget $3,223,605; $53,962 (1.7%) below budget, unfavorable")
                .contains("- Expenses: actual $2,960,379; budget $2,936,126; $24,254 (0.8%) over budget, unfavorable")
                .contains("$7,511 (0.5%) under budget, favorable");
    }
}
