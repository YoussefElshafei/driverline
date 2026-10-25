package com.driverline.planning.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SeasonalityTest {

    @ParameterizedTest
    @EnumSource(Seasonality.class)
    void factorsAverageToOneSoAnnualTotalIsPreserved(Seasonality seasonality) {
        BigDecimal sum = seasonality.monthlyFactors().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo("12");
    }

    @Test
    void monthOneIsJanuaryAndMonthTwelveIsDecember() {
        assertThat(Seasonality.UTILITIES.factorFor(1)).isEqualByComparingTo("1.30");
        assertThat(Seasonality.UTILITIES.factorFor(12)).isEqualByComparingTo("1.10");
    }

    @Test
    void specialtyCleaningPeaksInSpringAndIsQuietInJanuary() {
        assertThat(Seasonality.SPECIALTY.factorFor(5)).isGreaterThan(Seasonality.SPECIALTY.factorFor(1));
    }
}
