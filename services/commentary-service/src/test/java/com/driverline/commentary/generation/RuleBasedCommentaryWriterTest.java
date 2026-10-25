package com.driverline.commentary.generation;

import static com.driverline.commentary.SampleRequests.figures;
import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.commentary.SampleRequests;
import com.driverline.commentary.api.dto.AccountType;
import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.Favorability;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedCommentaryWriterTest {

    private final RuleBasedCommentaryWriter writer = new RuleBasedCommentaryWriter();

    @Test
    void summarisesTheTotalsAndListsTheLargestVariances() {
        String text = writer.write(SampleRequests.aceJanitorialFy2026());

        assertThat(text).isEqualTo("""
                FY2026, January to September: net income was $209,264 against a budget of $287,480, \
                $78,215 (27.2%) below plan, unfavorable. Revenue came in $53,962 (1.7%) below budget, unfavorable \
                and expenses came in $24,254 (0.8%) over budget, unfavorable.

                Largest variances:
                • Contract Cleaning Revenue (Sales & Account Management): $44,059 (1.6%) below budget, unfavorable.
                • Cleaning Supplies (Operations): $16,770 (7.3%) over budget, unfavorable.
                • Vehicle & Fuel (Fleet & Equipment): $14,454 (10.2%) over budget, unfavorable.
                • Wages - Cleaning Staff (Operations): $7,511 (0.5%) under budget, favorable.""");
    }

    @Test
    void revenueAboveBudgetIsDescribedAsAboveAndExpenseUnderBudgetAsUnder() {
        assertThat(RuleBasedCommentaryWriter.describe(
                figures("100.00", "110.00", "10.00", "10.0", Favorability.FAVORABLE), AccountType.REVENUE, "budget"))
                .isEqualTo("$10 (10.0%) above budget, favorable");
        assertThat(RuleBasedCommentaryWriter.describe(
                figures("100.00", "90.00", "-10.00", "-10.0", Favorability.FAVORABLE), AccountType.EXPENSE, "budget"))
                .isEqualTo("$10 (10.0%) under budget, favorable");
    }

    @Test
    void describesALineThatIsExactlyOnBudget() {
        assertThat(RuleBasedCommentaryWriter.describe(
                figures("100.00", "100.00", "0.00", "0.0", Favorability.ON_BUDGET), AccountType.EXPENSE, "budget"))
                .isEqualTo("exactly on budget");
    }

    @Test
    void saysWhenThereWasNoBudgetInsteadOfShowingAPercentage() {
        assertThat(RuleBasedCommentaryWriter.describe(
                figures("0.00", "250.00", "250.00", null, Favorability.UNFAVORABLE), AccountType.EXPENSE, "budget"))
                .isEqualTo("$250 (no budget set) over budget, unfavorable");
    }

    @Test
    void handlesAReportWithNoAccountVariances() {
        CommentaryRequest sample = SampleRequests.aceJanitorialFy2026();
        CommentaryRequest noHighlights = new CommentaryRequest(sample.periodLabel(), sample.revenue(),
                sample.expenses(), sample.netIncome(), List.of());

        assertThat(writer.write(noHighlights)).endsWith("No individual account variances to report.");
    }
}
