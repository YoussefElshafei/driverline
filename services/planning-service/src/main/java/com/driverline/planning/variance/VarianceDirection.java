package com.driverline.planning.variance;

import com.driverline.planning.account.AccountType;

/** Whether coming in above budget is good or bad for a line. */
public enum VarianceDirection {
    /** Revenue and net income: more than planned is favorable. */
    HIGHER_IS_BETTER,
    /** Expenses: more than planned is unfavorable. */
    LOWER_IS_BETTER;

    public static VarianceDirection forAccountType(AccountType type) {
        return switch (type) {
            case REVENUE -> HIGHER_IS_BETTER;
            case EXPENSE -> LOWER_IS_BETTER;
        };
    }
}
