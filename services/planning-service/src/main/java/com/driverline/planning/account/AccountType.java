package com.driverline.planning.account;

import java.math.BigDecimal;

public enum AccountType {
    REVENUE,
    EXPENSE;

    /**
     * The amount's effect on profit: revenue adds to profit, expenses subtract from it.
     * Used to net revenue and expenses into a department's contribution or company net income.
     */
    public BigDecimal profitImpact(BigDecimal amount) {
        return this == REVENUE ? amount : amount.negate();
    }
}
