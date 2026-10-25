package com.driverline.planning.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountTypeTest {

    @Test
    void revenueAddsToProfitAndExpensesSubtract() {
        assertThat(AccountType.REVENUE.profitImpact(new BigDecimal("100.00"))).isEqualTo(new BigDecimal("100.00"));
        assertThat(AccountType.EXPENSE.profitImpact(new BigDecimal("100.00"))).isEqualTo(new BigDecimal("-100.00"));
    }

    @Test
    void aNegativeExpenseSuchAsARefundAddsToProfit() {
        assertThat(AccountType.EXPENSE.profitImpact(new BigDecimal("-30.00"))).isEqualTo(new BigDecimal("30.00"));
    }
}
