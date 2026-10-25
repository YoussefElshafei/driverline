package com.driverline.commentary.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.driverline.commentary.SampleRequests;
import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.CommentaryResponse;
import com.driverline.commentary.llm.LanguageModel;
import com.driverline.commentary.llm.LanguageModelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** The language model is a Mockito mock, so every path (success, bad output, failure, disabled) runs offline. */
class CommentaryServiceTest {

    private final CommentaryRequest request = SampleRequests.aceJanitorialFy2026();
    private final String ruleBasedText = new RuleBasedCommentaryWriter().write(request);

    private LanguageModel model;
    private ObjectProvider<LanguageModel> provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        model = mock(LanguageModel.class);
        when(model.provider()).thenReturn("ollama");
        when(model.model()).thenReturn("llama3.1:8b");
        provider = mock(ObjectProvider.class);
    }

    private CommentaryService service() {
        return new CommentaryService(provider, new PromptBuilder(), new OutputGuard(), new RuleBasedCommentaryWriter());
    }

    @Test
    void returnsTheModelsTextWhenItPassesTheGuard() {
        when(provider.getIfAvailable()).thenReturn(model);
        when(model.generate(anyString(), anyString()))
                .thenReturn("Net income was $209,264, $78,215 below budget, mainly due to contract revenue.");

        CommentaryResponse response = service().generate(request);

        assertThat(response).isEqualTo(new CommentaryResponse(
                "Net income was $209,264, $78,215 below budget, mainly due to contract revenue.",
                "ollama", "llama3.1:8b", null));
    }

    @Test
    void fallsBackToRuleBasedTextWhenTheModelInventsAFigure() {
        when(provider.getIfAvailable()).thenReturn(model);
        when(model.generate(anyString(), anyString())).thenReturn("Net income missed budget by $90,000.");

        CommentaryResponse response = service().generate(request);

        assertThat(response.provider()).isEqualTo(CommentaryResponse.RULE_BASED);
        assertThat(response.text()).isEqualTo(ruleBasedText);
        assertThat(response.fallbackReason()).startsWith("The language model's response contained figures");
    }

    @Test
    void fallsBackToRuleBasedTextWhenTheModelIsUnavailable() {
        when(provider.getIfAvailable()).thenReturn(model);
        when(model.generate(anyString(), anyString()))
                .thenThrow(new LanguageModelException("Could not reach Ollama at http://localhost:11434"));

        CommentaryResponse response = service().generate(request);

        assertThat(response).isEqualTo(new CommentaryResponse(ruleBasedText, CommentaryResponse.RULE_BASED, null,
                "Could not reach Ollama at http://localhost:11434"));
    }

    @Test
    void usesRuleBasedTextWhenNoLanguageModelIsConfigured() {
        when(provider.getIfAvailable()).thenReturn(null);

        CommentaryResponse response = service().generate(request);

        assertThat(response.provider()).isEqualTo(CommentaryResponse.RULE_BASED);
        assertThat(response.fallbackReason()).isEqualTo(CommentaryService.LLM_DISABLED);
    }
}
