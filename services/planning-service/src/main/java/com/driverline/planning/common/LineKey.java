package com.driverline.planning.common;

import com.driverline.planning.period.FiscalPeriod;

/** Identifies one budget or actual cell: an account, in a department, for a month. Used to match rows to existing lines. */
public record LineKey(String accountCode, String departmentCode, FiscalPeriod period) {
}
