package com.driverline.commentary.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AmountsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "44059.04   | $44,059",
            "-44059.04  | $44,059",
            "0.50       | $1",
            "1234567.49 | $1,234,567",
            "0          | $0"
    })
    void formatsWholeDollarsWithoutSign(String amount, String expected) {
        assertThat(Amounts.dollars(new BigDecimal(amount))).isEqualTo(expected);
    }

    @Test
    void signedDollarsKeepTheMinus() {
        assertThat(Amounts.signedDollars(new BigDecimal("-53961.58"))).isEqualTo("-$53,962");
    }

    @Test
    void aMissingPercentageIsNotApplicable() {
        assertThat(Amounts.percent(null)).isEqualTo("n/a");
    }

    @Test
    void roundsToDollarsHalfUp() {
        assertThat(Amounts.roundedToDollars(new BigDecimal("-7511.50"))).isEqualTo(new BigDecimal("7512"));
    }
}
