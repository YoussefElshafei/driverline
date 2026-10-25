package com.driverline.planning.forecast;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.dto.ForecastDepartment;
import com.driverline.planning.forecast.dto.ForecastLineRow;
import com.driverline.planning.forecast.dto.ForecastReport;
import com.driverline.planning.forecast.dto.ForecastSummary;
import com.driverline.planning.forecast.dto.OutlookLine;
import com.driverline.planning.forecast.engine.ForecastedLine;
import com.driverline.planning.variance.VarianceCalculator;
import com.driverline.planning.variance.VarianceDirection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Turns forecasted lines into the report: lines grouped by department, department net contribution,
 * and company totals. Outlook vs. budget uses the same {@link VarianceCalculator} rules as the variance reports.
 */
@Component
public class ForecastReportBuilder {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    public ForecastReport build(int year, Scenario scenario, int latestActualMonth, List<ForecastedLine> lines) {
        Map<String, List<ForecastedLine>> byDepartment = lines.stream()
                .collect(Collectors.groupingBy(ForecastedLine::departmentCode, TreeMap::new, Collectors.toList()));

        List<ForecastDepartment> departments = byDepartment.values().stream()
                .map(departmentLines -> new ForecastDepartment(
                        departmentLines.getFirst().departmentCode(),
                        departmentLines.getFirst().departmentName(),
                        netLine(departmentLines),
                        departmentLines.stream().map(ForecastReportBuilder::toRow).toList()))
                .toList();

        return new ForecastReport(year, scenario.getCode(), scenario.getName(), latestActualMonth,
                summarize(lines), departments);
    }

    public ForecastSummary summarize(List<ForecastedLine> lines) {
        List<ForecastedLine> revenue = filter(lines, line -> line.accountType() == AccountType.REVENUE);
        List<ForecastedLine> expenses = filter(lines, line -> line.accountType() == AccountType.EXPENSE);
        return new ForecastSummary(
                outlook(sum(revenue, ForecastedLine::budgetTotal), sum(revenue, ForecastedLine::outlookTotal),
                        VarianceDirection.HIGHER_IS_BETTER),
                outlook(sum(expenses, ForecastedLine::budgetTotal), sum(expenses, ForecastedLine::outlookTotal),
                        VarianceDirection.LOWER_IS_BETTER),
                netLine(lines));
    }

    private static ForecastLineRow toRow(ForecastedLine line) {
        return new ForecastLineRow(line.accountCode(), line.accountName(), line.accountType(), line.method(),
                line.months(),
                outlook(line.budgetTotal(), line.outlookTotal(), VarianceDirection.forAccountType(line.accountType())));
    }

    private static OutlookLine netLine(List<ForecastedLine> lines) {
        return outlook(
                sum(lines, line -> line.accountType().profitImpact(line.budgetTotal())),
                sum(lines, line -> line.accountType().profitImpact(line.outlookTotal())),
                VarianceDirection.HIGHER_IS_BETTER);
    }

    private static OutlookLine outlook(BigDecimal budget, BigDecimal outlook, VarianceDirection direction) {
        return OutlookLine.from(VarianceCalculator.calculate(budget, outlook, direction));
    }

    private static BigDecimal sum(List<ForecastedLine> lines, Function<ForecastedLine, BigDecimal> amount) {
        return lines.stream().map(amount).reduce(ZERO, BigDecimal::add);
    }

    private static List<ForecastedLine> filter(List<ForecastedLine> lines, Predicate<ForecastedLine> predicate) {
        return lines.stream().filter(predicate).toList();
    }
}
