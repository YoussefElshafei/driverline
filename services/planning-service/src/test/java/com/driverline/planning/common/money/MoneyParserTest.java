package com.driverline.planning.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.common.money.MoneyParser.InvalidMoneyException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyParserTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "1234.56      | 1234.56",
            "1234.5       | 1234.50",
            "1234         | 1234.00",
            "0            | 0.00",
            "1,234.56     | 1234.56",
            "$1,234.56    | 1234.56",
            "-1234.56     | -1234.56",
            "(1,234.56)   | -1234.56",
            "'  42.10  '  | 42.10",
            "1.230        | 1.23",
            "9999999999999.99 | 9999999999999.99"
    })
    void parsesCommonSpreadsheetFormatsToExactlyTwoDecimals(String input, String expected) {
        assertThat(MoneyParser.parse(input)).isEqualTo(expected).hasToString(expected);
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "12.345           | has more than 2 decimal places",
            "0.001            | has more than 2 decimal places",
            "abc              | is not a valid amount",
            "12.3.4           | is not a valid amount",
            "1e5              | is not a valid amount",
            "(-5.00)          | is not a valid amount",
            "10000000000000.00 | is too large"
    })
    void rejectsInvalidAmountsInsteadOfRounding(String input, String expectedMessage) {
        assertThatThrownBy(() -> MoneyParser.parse(input))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessage(expectedMessage);
    }
}
