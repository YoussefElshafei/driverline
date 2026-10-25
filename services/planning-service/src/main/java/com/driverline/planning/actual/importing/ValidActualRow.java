package com.driverline.planning.actual.importing;

import com.driverline.planning.common.LineKey;
import com.driverline.planning.period.FiscalPeriod;
import java.math.BigDecimal;

/** A row that passed validation: typed values, known codes, money at exactly 2 decimals. */
public record ValidActualRow(
        int rowNumber, String accountCode, String departmentCode, FiscalPeriod period, BigDecimal amount) {

    public LineKey key() {
        return new LineKey(accountCode, departmentCode, period);
    }
}
