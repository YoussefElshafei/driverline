package com.driverline.planning.variance.dto;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.variance.VarianceLine;

/** One account's budget vs. actual, with amounts in their natural sign and type-aware favorability. */
public record AccountVarianceRow(String accountCode, String accountName, AccountType accountType, VarianceLine line) {
}
