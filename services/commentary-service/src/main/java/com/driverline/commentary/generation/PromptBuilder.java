package com.driverline.commentary.generation;

import static com.driverline.commentary.generation.Amounts.signedDollars;

import com.driverline.commentary.api.dto.AccountType;
import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.VarianceFigures;
import com.driverline.commentary.api.dto.VarianceHighlight;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Builds the instructions and data for the language model. The data is given pre-formatted ("$44,059"), and the
 * model is told to copy figures exactly, so {@link OutputGuard} can check that it didn't invent any.
 */
@Component
public class PromptBuilder {

    public record Prompt(String system, String user) {
    }

    static final String SYSTEM_PROMPT = """
            You are a financial analyst writing budget vs. actual variance commentary for a small company's \
            leadership team.
            Rules:
            - Use only the figures in the data. Copy every dollar amount and percentage exactly as written, \
            for example $44,059 or 1.6%. Do not calculate new numbers.
            - Do not invent reasons for a variance. If the data does not say why it happened, say what \
            management should look into.
            - Favorable means the variance helps profit; unfavorable means it hurts profit.
            - Write two short paragraphs followed by up to four bullet points that start with "- ".
            - Keep it under 180 words. Plain text only: no headings, tables or bold text.""";

    public Prompt build(CommentaryRequest request) {
        StringBuilder data = new StringBuilder();
        data.append("Period: ").append(request.periodLabel()).append("\n\n");
        data.append("Company totals:\n");
        data.append(line("Revenue", request.revenue(), AccountType.REVENUE)).append('\n');
        data.append(line("Expenses", request.expenses(), AccountType.EXPENSE)).append('\n');
        data.append(line("Net income", request.netIncome(), AccountType.REVENUE)).append("\n\n");

        if (request.highlights().isEmpty()) {
            data.append("Largest account variances: none.\n");
        } else {
            data.append("Largest account variances, biggest first:\n");
            int rank = 1;
            for (VarianceHighlight highlight : request.highlights()) {
                String label = "%d. %s (%s, %s account)".formatted(rank++, highlight.accountName(),
                        highlight.departmentName(), highlight.accountType().name().toLowerCase(Locale.ROOT));
                data.append(line(label, highlight.figures(), highlight.accountType())).append('\n');
            }
        }
        data.append("\nWrite the commentary now.");
        return new Prompt(SYSTEM_PROMPT, data.toString());
    }

    /**
     * The direction is spelled out in words ("$53,962 (1.7%) below budget, unfavorable") rather than left to a
     * minus sign: small models misread signs far more often than words.
     */
    private static String line(String label, VarianceFigures figures, AccountType type) {
        return "- %s: actual %s; budget %s; %s".formatted(label,
                signedDollars(figures.actual()), signedDollars(figures.budget()),
                RuleBasedCommentaryWriter.describe(figures, type, "budget"));
    }
}
