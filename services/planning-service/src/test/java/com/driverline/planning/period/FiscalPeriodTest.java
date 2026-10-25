package com.driverline.planning.period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FiscalPeriodTest {

    @Test
    void acceptsFirstAndLastMonthOfTheYear() {
        assertThat(FiscalPeriod.of(2026, 1).month()).isEqualTo(1);
        assertThat(FiscalPeriod.of(2026, 12).month()).isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 13})
    void rejectsMonthsOutsideOneToTwelve(int month) {
        assertThatThrownBy(() -> FiscalPeriod.of(2026, month))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fiscal month");
    }

    @ParameterizedTest
    @ValueSource(ints = {1999, 2101})
    void rejectsYearsOutsideSupportedRange(int year) {
        assertThatThrownBy(() -> FiscalPeriod.of(year, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fiscal year");
    }

    @Test
    void periodsWithSameYearAndMonthAreEqual() {
        assertThat(FiscalPeriod.of(2025, 3)).isEqualTo(FiscalPeriod.of(2025, 3));
    }
}
