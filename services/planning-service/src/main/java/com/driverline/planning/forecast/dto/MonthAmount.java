package com.driverline.planning.forecast.dto;

import java.math.BigDecimal;

/** @param actual true when the amount is a recorded actual, false when it is forecast */
public record MonthAmount(int month, BigDecimal amount, boolean actual) {
}
