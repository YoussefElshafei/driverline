package com.driverline.commentary.generation;

import static com.driverline.commentary.generation.Amounts.dollars;
import static com.driverline.commentary.generation.Amounts.percent;

import com.driverline.commentary.api.dto.AccountType;
import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.Favorability;
import com.driverline.commentary.api.dto.VarianceFigures;
import com.driverline.commentary.api.dto.VarianceHighlight;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Writes plain-English commentary from the numbers alone, with no language model. It is deterministic, always
 * available, and used whenever the language model is off, unreachable, slow, or returns unusable text.
 */
@Component
public class RuleBasedCommentaryWriter {

    public String write(CommentaryRequest request) {
        StringBuilder text = new StringBuilder();
        text.append(request.periodLabel()).append(": net income was ").append(dollars(request.netIncome().actual()))
                .append(" against a budget of ").append(dollars(request.netIncome().budget())).append(", ")
                .append(describe(request.netIncome(), AccountType.REVENUE, "plan")).append(". ");
        text.append("Revenue came in ").append(describe(request.revenue(), AccountType.REVENUE, "budget"))
                .append(" and expenses came in ").append(describe(request.expenses(), AccountType.EXPENSE, "budget"))
                .append('.');

        if (request.highlights().isEmpty()) {
            text.append("\n\nNo individual account variances to report.");
            return text.toString();
        }
        text.append("\n\nLargest variances:");
        for (VarianceHighlight highlight : request.highlights()) {
            text.append("\n• ").append(highlight.accountName()).append(" (").append(highlight.departmentName()).append("): ")
                    .append(describe(highlight.figures(), highlight.accountType(), "budget")).append('.');
        }
        return text.toString();
    }

    /**
     * e.g. "$44,059 (1.6%) below budget, unfavorable". Revenue and net figures are "above/below";
     * expenses are "over/under", the way finance people usually say it.
     */
    static String describe(VarianceFigures figures, AccountType type, String target) {
        if (figures.favorability() == Favorability.ON_BUDGET) {
            return "exactly on " + target;
        }
        boolean higher = figures.variance().signum() > 0;
        String direction = type == AccountType.EXPENSE ? (higher ? "over" : "under") : (higher ? "above" : "below");
        String size = figures.variancePercent() == null
                ? dollars(figures.variance()) + " (no budget set)"
                : dollars(figures.variance()) + " (" + percent(figures.variancePercent()) + ")";
        return size + " " + direction + " " + target + ", " + figures.favorability().name().toLowerCase(Locale.ROOT);
    }
}
