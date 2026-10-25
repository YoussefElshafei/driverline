package com.driverline.planning.forecast;

/** How a line's open months are forecast. */
public enum ForecastMethod {
    /** quantity x rate, e.g. 41 cleaners x $3,850 per cleaner per month. */
    UNITS_X_RATE,
    /** percent of the month's forecast total revenue, e.g. supplies = 7.6% of revenue. Expense lines only. */
    PERCENT_OF_REVENUE,
    /** the month's budget amount; the default for lines without drivers (rent, insurance, ...). */
    BUDGET
}
