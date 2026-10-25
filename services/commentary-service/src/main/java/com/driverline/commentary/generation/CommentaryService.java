package com.driverline.commentary.generation;

import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.CommentaryResponse;
import com.driverline.commentary.generation.OutputGuard.Accepted;
import com.driverline.commentary.generation.OutputGuard.Rejected;
import com.driverline.commentary.generation.PromptBuilder.Prompt;
import com.driverline.commentary.llm.LanguageModel;
import com.driverline.commentary.llm.LanguageModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Produces commentary: from the language model when one is configured and its answer passes the
 * {@link OutputGuard}, otherwise from the {@link RuleBasedCommentaryWriter}. Users always get commentary,
 * and the response says which source wrote it and why.
 */
@Service
public class CommentaryService {

    private static final Logger log = LoggerFactory.getLogger(CommentaryService.class);
    static final String LLM_DISABLED = "AI commentary is turned off (COMMENTARY_LLM_PROVIDER=none)";

    private final ObjectProvider<LanguageModel> languageModel;
    private final PromptBuilder promptBuilder;
    private final OutputGuard outputGuard;
    private final RuleBasedCommentaryWriter ruleBasedWriter;

    public CommentaryService(ObjectProvider<LanguageModel> languageModel, PromptBuilder promptBuilder,
            OutputGuard outputGuard, RuleBasedCommentaryWriter ruleBasedWriter) {
        this.languageModel = languageModel;
        this.promptBuilder = promptBuilder;
        this.outputGuard = outputGuard;
        this.ruleBasedWriter = ruleBasedWriter;
    }

    public CommentaryResponse generate(CommentaryRequest request) {
        LanguageModel model = languageModel.getIfAvailable();
        if (model == null) {
            return ruleBased(request, LLM_DISABLED);
        }
        try {
            Prompt prompt = promptBuilder.build(request);
            String output = model.generate(prompt.system(), prompt.user());
            return switch (outputGuard.check(output, request)) {
                case Accepted accepted -> new CommentaryResponse(accepted.text(), model.provider(), model.model(), null);
                case Rejected rejected -> {
                    log.warn("Rejected {} output: {}", model.model(), rejected.reason());
                    yield ruleBased(request, capitalize(rejected.reason()));
                }
            };
        } catch (LanguageModelException e) {
            log.warn("Language model unavailable, using rule-based commentary: {}", e.getMessage());
            return ruleBased(request, e.getMessage());
        }
    }

    private CommentaryResponse ruleBased(CommentaryRequest request, String reason) {
        return new CommentaryResponse(ruleBasedWriter.write(request), CommentaryResponse.RULE_BASED, null, reason);
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
