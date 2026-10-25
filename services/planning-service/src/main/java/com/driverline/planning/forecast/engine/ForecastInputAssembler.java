package com.driverline.planning.forecast.engine;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.ForecastDriverValue;
import com.driverline.planning.forecast.ForecastLine;
import com.driverline.planning.forecast.ForecastLineKey;
import com.driverline.planning.variance.report.PlanningFact;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * Combines a year's budgets and actuals with the forecast configuration for one scenario into engine inputs.
 * Every line with a budget, an actual or a forecast configuration is included; unconfigured lines use their budget.
 */
@Component
public class ForecastInputAssembler {

    private static final Comparator<ForecastLineKey> LINE_ORDER = Comparator
            .comparing(ForecastLineKey::departmentCode)
            .thenComparing(ForecastLineKey::accountCode);

    public List<ForecastInputLine> assemble(
            List<PlanningFact> facts, List<ForecastLine> forecastLines, String scenarioCode) {
        Map<ForecastLineKey, LineBuilder> builders = new TreeMap<>(LINE_ORDER);

        for (PlanningFact fact : facts) {
            LineBuilder builder = builders.computeIfAbsent(
                    new ForecastLineKey(fact.accountCode(), fact.departmentCode()),
                    key -> new LineBuilder(fact.accountName(), fact.accountType(), fact.departmentName()));
            builder.budgets.put(fact.month(), fact.budget());
            if (fact.hasActual()) {
                builder.actuals.put(fact.month(), fact.actual());
            }
        }

        for (ForecastLine line : forecastLines) {
            LineBuilder builder = builders.computeIfAbsent(
                    new ForecastLineKey(line.getAccount().getCode(), line.getDepartment().getCode()),
                    key -> new LineBuilder(line.getAccount().getName(), line.getAccount().getType(),
                            line.getDepartment().getName()));
            builder.driver = driverFor(line, scenarioCode);
        }

        return builders.entrySet().stream()
                .map(entry -> entry.getValue().build(entry.getKey()))
                .toList();
    }

    /** A driver-based line without values for this scenario falls back to its budget rather than failing. */
    private static DriverSpec driverFor(ForecastLine line, String scenarioCode) {
        Optional<ForecastDriverValue> value = line.valueFor(scenarioCode);
        return switch (line.getMethod()) {
            case UNITS_X_RATE -> value
                    .filter(v -> v.getQuantity() != null && v.getRate() != null)
                    .map(v -> DriverSpec.unitsTimesRate(v.getQuantity(), v.getRate()))
                    .orElse(DriverSpec.USE_BUDGET);
            case PERCENT_OF_REVENUE -> value
                    .filter(v -> v.getPercent() != null)
                    .map(v -> DriverSpec.percentOfRevenue(v.getPercent()))
                    .orElse(DriverSpec.USE_BUDGET);
            case BUDGET -> DriverSpec.USE_BUDGET;
        };
    }

    private static final class LineBuilder {

        private final String accountName;
        private final AccountType accountType;
        private final String departmentName;
        private final Map<Integer, BigDecimal> budgets = new HashMap<>();
        private final Map<Integer, BigDecimal> actuals = new HashMap<>();
        private DriverSpec driver = DriverSpec.USE_BUDGET;

        LineBuilder(String accountName, AccountType accountType, String departmentName) {
            this.accountName = accountName;
            this.accountType = accountType;
            this.departmentName = departmentName;
        }

        ForecastInputLine build(ForecastLineKey key) {
            return new ForecastInputLine(key.accountCode(), accountName, accountType, key.departmentCode(),
                    departmentName, budgets, actuals, driver);
        }
    }
}
