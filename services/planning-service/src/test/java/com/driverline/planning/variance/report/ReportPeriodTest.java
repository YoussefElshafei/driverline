package com.driverline.planning.variance.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.common.error.RequestValidationException;
import org.junit.jupiter.api.Test;

class ReportPeriodTest {

    @Test
    void defaultsToJanuaryThroughTheLatestMonthWithActuals() {
        assertThat(ReportPeriod.resolve(2026, null, null, 9)).isEqualTo(new ReportPeriod(2026, 1, 9));
    }

    @Test
    void defaultsToTheWholeYearWhenThereAreNoActualsYet() {
        assertThat(ReportPeriod.resolve(2027, null, null, 0)).isEqualTo(new ReportPeriod(2027, 1, 12));
    }

    @Test
    void keepsExplicitlyRequestedMonths() {
        assertThat(ReportPeriod.resolve(2026, 4, 6, 9)).isEqualTo(new ReportPeriod(2026, 4, 6));
    }

    @Test
    void rejectsARangeThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> ReportPeriod.resolve(2026, 10, 3, 9))
                .isInstanceOfSatisfying(RequestValidationException.class, e ->
                        assertThat(e.getErrors().getFirst().field()).isEqualTo("toMonth"));
    }

    @Test
    void includesBothEndsOfTheRange() {
        ReportPeriod q2 = new ReportPeriod(2026, 4, 6);

        assertThat(q2.includes(3)).isFalse();
        assertThat(q2.includes(4)).isTrue();
        assertThat(q2.includes(6)).isTrue();
        assertThat(q2.includes(7)).isFalse();
    }
}
