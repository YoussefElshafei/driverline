package com.driverline.planning.seed;

import static com.driverline.planning.forecast.ForecastMethod.PERCENT_OF_REVENUE;
import static com.driverline.planning.forecast.ForecastMethod.UNITS_X_RATE;
import static com.driverline.planning.seed.AceJanitorialProfile.ADMIN;
import static com.driverline.planning.seed.AceJanitorialProfile.CLEANING_SUPPLIES;
import static com.driverline.planning.seed.AceJanitorialProfile.CLEANING_WAGES;
import static com.driverline.planning.seed.AceJanitorialProfile.CONTRACT_REVENUE;
import static com.driverline.planning.seed.AceJanitorialProfile.FLEET;
import static com.driverline.planning.seed.AceJanitorialProfile.OPERATIONS;
import static com.driverline.planning.seed.AceJanitorialProfile.SALARIES;
import static com.driverline.planning.seed.AceJanitorialProfile.SALES;
import static com.driverline.planning.seed.AceJanitorialProfile.SPECIALTY_REVENUE;
import static com.driverline.planning.seed.AceJanitorialProfile.VEHICLE_AND_FUEL;

import com.driverline.planning.forecast.ForecastMethod;
import java.math.BigDecimal;
import java.util.List;

/**
 * FY2026 forecast drivers for the fictional Ace Janitorial Service, set after September close.
 * Base follows recent actuals (a lost contract, higher fuel and supply costs); Best and Worst move the key drivers
 * up and down. Lines not listed here (rent, insurance, software, utilities, marketing, equipment) use their budget.
 */
public final class AceJanitorialForecastDrivers {

    public static final int YEAR = 2026;

    /** Driver values for Base, Best and Worst. Quantity and rate for units x rate; percent for percent of revenue. */
    public record Values(BigDecimal quantity, BigDecimal rate, BigDecimal percent) {
    }

    public record DriverSeed(String accountCode, String departmentCode, ForecastMethod method,
            String quantityLabel, String rateLabel, Values base, Values best, Values worst) {
    }

    private AceJanitorialForecastDrivers() {
    }

    public static List<DriverSeed> drivers() {
        return List.of(
                units(CONTRACT_REVENUE, SALES, "Active contracts", "Avg monthly contract value",
                        "62", "4700.00", "64", "4725.00", "60", "4650.00"),
                units(SPECIALTY_REVENUE, SALES, "Specialty jobs per month", "Avg job price",
                        "16", "2900.00", "18", "3000.00", "13", "2850.00"),
                units(CLEANING_WAGES, OPERATIONS, "Cleaners (FTE)", "Monthly cost per cleaner",
                        "41", "3850.00", "42", "3850.00", "40", "3950.00"),
                units(SALARIES, SALES, "Salaried staff", "Avg monthly salary",
                        "2", "8400.00", "2", "8400.00", "2", "8400.00"),
                units(SALARIES, OPERATIONS, "Salaried staff", "Avg monthly salary",
                        "4", "7350.00", "4", "7350.00", "4", "7350.00"),
                units(SALARIES, FLEET, "Salaried staff", "Avg monthly salary",
                        "1", "7350.00", "1", "7350.00", "1", "7350.00"),
                units(SALARIES, ADMIN, "Salaried staff", "Avg monthly salary",
                        "3", "9100.00", "3", "9100.00", "3", "9100.00"),
                new DriverSeed(CLEANING_SUPPLIES, OPERATIONS, PERCENT_OF_REVENUE, null, null,
                        percent("7.6"), percent("7.2"), percent("8.0")),
                units(VEHICLE_AND_FUEL, FLEET, "Vans", "Monthly cost per van",
                        "14", "1250.00", "14", "1150.00", "14", "1350.00"));
    }

    private static DriverSeed units(String account, String department, String quantityLabel, String rateLabel,
            String baseQty, String baseRate, String bestQty, String bestRate, String worstQty, String worstRate) {
        return new DriverSeed(account, department, UNITS_X_RATE, quantityLabel, rateLabel,
                new Values(new BigDecimal(baseQty), new BigDecimal(baseRate), null),
                new Values(new BigDecimal(bestQty), new BigDecimal(bestRate), null),
                new Values(new BigDecimal(worstQty), new BigDecimal(worstRate), null));
    }

    private static Values percent(String percent) {
        return new Values(null, null, new BigDecimal(percent));
    }
}
