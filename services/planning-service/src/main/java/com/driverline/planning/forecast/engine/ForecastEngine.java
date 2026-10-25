package com.driverline.planning.forecast.engine;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.ForecastMethod;
import com.driverline.planning.forecast.dto.MonthAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * Produces a full-year outlook per line: actuals for closed months, driver-based forecasts for open months.
 *
 * <p>Two passes, because percent-of-revenue lines depend on revenue: first every line that doesn't
 * (revenue lines, units x rate, budget), then the percent-of-revenue lines using each month's forecast revenue.
 * Pure logic with no database access.
 */
@Component
public class ForecastEngine {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public List<ForecastedLine> forecast(List<ForecastInputLine> lines, Set<Integer> closedMonths) {
        // Fail fast, before any calculation: revenue can't depend on revenue.
        lines.forEach(ForecastEngine::requireNoCircularRevenue);
        List<List<MonthAmount>> monthsByIndex = new ArrayList<>(Collections.nCopies(lines.size(), null));

        for (int i = 0; i < lines.size(); i++) {
            ForecastInputLine line = lines.get(i);
            if (line.driver().method() != ForecastMethod.PERCENT_OF_REVENUE) {
                monthsByIndex.set(i, months(line, closedMonths, month -> independentForecast(line, month)));
            }
        }

        Map<Integer, BigDecimal> revenueByMonth = forecastRevenueByMonth(lines, monthsByIndex);
        for (int i = 0; i < lines.size(); i++) {
            ForecastInputLine line = lines.get(i);
            if (line.driver().method() == ForecastMethod.PERCENT_OF_REVENUE) {
                monthsByIndex.set(i, months(line, closedMonths,
                        month -> percentOf(revenueByMonth.getOrDefault(month, ZERO), line.driver().percent())));
            }
        }

        return IntStream.range(0, lines.size())
                .mapToObj(i -> toForecastedLine(lines.get(i), monthsByIndex.get(i)))
                .toList();
    }

    private static List<MonthAmount> months(
            ForecastInputLine line, Set<Integer> closedMonths, IntFunction<BigDecimal> forecast) {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> closedMonths.contains(month)
                        // A closed month uses its actual; nothing recorded in a closed month means zero.
                        ? new MonthAmount(month, line.actualByMonth().getOrDefault(month, ZERO), true)
                        : new MonthAmount(month, forecast.apply(month), false))
                .toList();
    }

    private static BigDecimal independentForecast(ForecastInputLine line, int month) {
        DriverSpec driver = line.driver();
        return switch (driver.method()) {
            case UNITS_X_RATE -> driver.quantity().multiply(driver.rate()).setScale(2, RoundingMode.HALF_UP);
            case BUDGET -> line.budgetByMonth().getOrDefault(month, ZERO);
            case PERCENT_OF_REVENUE -> throw new IllegalStateException("Percent-of-revenue lines are forecast in the second pass");
        };
    }

    /** Total revenue per open month, from the first pass. Closed months aren't needed for percent-of-revenue. */
    private static Map<Integer, BigDecimal> forecastRevenueByMonth(
            List<ForecastInputLine> lines, List<List<MonthAmount>> monthsByIndex) {
        Map<Integer, BigDecimal> totals = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).accountType() == AccountType.REVENUE) {
                monthsByIndex.get(i).stream()
                        .filter(month -> !month.actual())
                        .forEach(month -> totals.merge(month.month(), month.amount(), BigDecimal::add));
            }
        }
        return totals;
    }

    private static BigDecimal percentOf(BigDecimal revenue, BigDecimal percent) {
        return revenue.multiply(percent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static void requireNoCircularRevenue(ForecastInputLine line) {
        if (line.accountType() == AccountType.REVENUE && line.driver().method() == ForecastMethod.PERCENT_OF_REVENUE) {
            throw new IllegalArgumentException(
                    "Revenue line %s cannot be a percentage of revenue".formatted(line.accountCode()));
        }
    }

    private static ForecastedLine toForecastedLine(ForecastInputLine line, List<MonthAmount> months) {
        BigDecimal budgetTotal = line.budgetByMonth().values().stream().reduce(ZERO, BigDecimal::add);
        BigDecimal outlookTotal = months.stream().map(MonthAmount::amount).reduce(ZERO, BigDecimal::add);
        return new ForecastedLine(
                line.accountCode(), line.accountName(), line.accountType(),
                line.departmentCode(), line.departmentName(),
                line.driver().method(), months, budgetTotal, outlookTotal);
    }
}
