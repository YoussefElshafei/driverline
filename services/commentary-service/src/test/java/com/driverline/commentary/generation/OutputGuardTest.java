package com.driverline.commentary.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.commentary.SampleRequests;
import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.generation.OutputGuard.Accepted;
import com.driverline.commentary.generation.OutputGuard.Rejected;
import com.driverline.commentary.generation.OutputGuard.Result;
import org.junit.jupiter.api.Test;

class OutputGuardTest {

    private final OutputGuard guard = new OutputGuard();
    private final CommentaryRequest request = SampleRequests.aceJanitorialFy2026();

    @Test
    void acceptsTextThatOnlyUsesFiguresFromTheData() {
        Result result = guard.check("""
                In FY2026 net income was $209,264, which is $78,215 (27.2%) below the $287,480 budget.
                - Contract Cleaning Revenue fell $44,059.04 short (1.6%).
                - Vehicle & Fuel was 14,454 over budget.""", request);

        assertThat(result).isInstanceOf(Accepted.class);
    }

    @Test
    void rejectsAFigureThatIsNotInTheData() {
        Result result = guard.check("Net income was $209,264, about $50,000 below budget.", request);

        assertThat(result).isInstanceOfSatisfying(Rejected.class, rejected ->
                assertThat(rejected.reason()).contains("not in the data").contains("50,000"));
    }

    @Test
    void ignoresSmallNumbersSuchAsPercentagesAndCounts() {
        Result result = guard.check("Revenue was 1.7% below budget across 4 departments and 3 quarters.", request);

        assertThat(result).isInstanceOf(Accepted.class);
    }

    @Test
    void stripsReasoningTagsThatSomeModelsAddBeforeTheAnswer() {
        Result result = guard.check("<think>Maybe say 99,999?</think>\n\nNet income was $209,264.", request);

        assertThat(result).isInstanceOfSatisfying(Accepted.class, accepted ->
                assertThat(accepted.text()).isEqualTo("Net income was $209,264."));
    }

    @Test
    void rejectsAnEmptyResponse() {
        assertThat(guard.check("  <think>hmm</think>  ", request)).isInstanceOf(Rejected.class);
        assertThat(guard.check(null, request)).isInstanceOf(Rejected.class);
    }

    @Test
    void rejectsAResponseThatIsFarTooLong() {
        assertThat(guard.check("word ".repeat(500), request)).isInstanceOfSatisfying(Rejected.class, rejected ->
                assertThat(rejected.reason()).contains("too long"));
    }
}
