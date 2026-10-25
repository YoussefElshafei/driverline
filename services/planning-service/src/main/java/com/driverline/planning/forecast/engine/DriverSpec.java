package com.driverline.planning.forecast.engine;

import com.driverline.planning.forecast.ForecastMethod;
import java.math.BigDecimal;

/** The method and driver values used to forecast one line in one scenario. */
public record DriverSpec(ForecastMethod method, BigDecimal quantity, BigDecimal rate, BigDecimal percent) {

    public static final DriverSpec USE_BUDGET = new DriverSpec(ForecastMethod.BUDGET, null, null, null);

    public static DriverSpec unitsTimesRate(BigDecimal quantity, BigDecimal rate) {
        return new DriverSpec(ForecastMethod.UNITS_X_RATE, quantity, rate, null);
    }

    public static DriverSpec percentOfRevenue(BigDecimal percent) {
        return new DriverSpec(ForecastMethod.PERCENT_OF_REVENUE, null, null, percent);
    }
}
