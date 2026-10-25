package com.driverline.planning.variance.report;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.variance.VarianceCalculator;
import com.driverline.planning.variance.VarianceDirection;
import com.driverline.planning.variance.VarianceLine;
import com.driverline.planning.variance.dto.AccountVarianceReport;
import com.driverline.planning.variance.dto.AccountVarianceRow;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceRow;
import com.driverline.planning.variance.dto.MonthlyVarianceRow;
import com.driverline.planning.variance.dto.VarianceSummary;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * Builds variance reports from planning facts. Pure logic with no database access, so every rule is unit-testable.
 *
 * <p>Within the selected months, a missing actual counts as zero: those months are closed, so nothing recorded
 * means nothing was earned or spent. Months without any actuals at all are reported as not closed.
 */
@Component
public class VarianceReportBuilder {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    public DepartmentVarianceReport byDepartment(List<PlanningFact> facts, ReportPeriod period) {
        List<PlanningFact> inPeriod = inPeriod(facts, period);

        Map<String, List<PlanningFact>> byDepartment = groupSorted(inPeriod, PlanningFact::departmentCode);
        List<DepartmentVarianceRow> departments = byDepartment.values().stream()
                .map(departmentFacts -> new DepartmentVarianceRow(
                        departmentFacts.getFirst().departmentCode(),
                        departmentFacts.getFirst().departmentName(),
                        netLine(departmentFacts),
                        accountRows(departmentFacts)))
                .toList();

        return new DepartmentVarianceReport(
                period.year(), period.fromMonth(), period.toMonth(), latestActualMonth(facts),
                summary(inPeriod), departments);
    }

    public AccountVarianceReport byAccount(List<PlanningFact> facts, ReportPeriod period) {
        List<PlanningFact> inPeriod = inPeriod(facts, period);
        List<AccountVarianceRow> rows = accountRows(inPeriod);

        return new AccountVarianceReport(
                period.year(), period.fromMonth(), period.toMonth(), latestActualMonth(facts),
                summary(inPeriod),
                rows.stream().filter(row -> row.accountType() == AccountType.REVENUE).toList(),
                rows.stream().filter(row -> row.accountType() == AccountType.EXPENSE).toList());
    }

    /**
     * @param accountFilter when null, amounts are netted into net income (revenue - expenses) and higher is better;
     *                      when set, amounts keep their natural sign and the account type decides favorability
     */
    public MonthlyTrend byMonth(List<PlanningFact> facts, String departmentFilter, String accountFilter,
            VarianceDirection direction) {
        Set<Integer> closedMonths = closedMonths(facts);
        List<PlanningFact> scoped = facts.stream()
                .filter(fact -> departmentFilter == null || fact.departmentCode().equals(departmentFilter))
                .filter(fact -> accountFilter == null || fact.accountCode().equals(accountFilter))
                .toList();
        Function<PlanningFact, BigDecimal> budgetOf = accountFilter == null ? VarianceReportBuilder::signedBudget : PlanningFact::budget;
        Function<PlanningFact, BigDecimal> actualOf = accountFilter == null ? VarianceReportBuilder::signedActual : VarianceReportBuilder::actualOrZero;

        List<MonthlyVarianceRow> months = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    List<PlanningFact> monthFacts = scoped.stream().filter(fact -> fact.month() == month).toList();
                    BigDecimal budget = sum(monthFacts, budgetOf);
                    boolean closed = closedMonths.contains(month);
                    VarianceLine variance = closed
                            ? VarianceCalculator.calculate(budget, sum(monthFacts, actualOf), direction)
                            : null;
                    return new MonthlyVarianceRow(month, budget, closed, variance);
                })
                .toList();

        List<PlanningFact> closedFacts = scoped.stream().filter(fact -> closedMonths.contains(fact.month())).toList();
        VarianceLine closedTotal = VarianceCalculator.calculate(
                sum(closedFacts, budgetOf), sum(closedFacts, actualOf), direction);
        return new MonthlyTrend(months, closedTotal, latestActualMonth(facts));
    }

    public record MonthlyTrend(List<MonthlyVarianceRow> months, VarianceLine closedMonthsTotal, int latestActualMonth) {
    }

    public int latestActualMonth(List<PlanningFact> facts) {
        return closedMonths(facts).stream().max(Integer::compare).orElse(0);
    }

    private static Set<Integer> closedMonths(List<PlanningFact> facts) {
        return facts.stream().filter(PlanningFact::hasActual).map(PlanningFact::month).collect(Collectors.toSet());
    }

    private static List<PlanningFact> inPeriod(List<PlanningFact> facts, ReportPeriod period) {
        return facts.stream().filter(fact -> period.includes(fact.month())).toList();
    }

    private static List<AccountVarianceRow> accountRows(List<PlanningFact> facts) {
        return groupSorted(facts, PlanningFact::accountCode).values().stream()
                .map(accountFacts -> {
                    PlanningFact first = accountFacts.getFirst();
                    VarianceLine line = VarianceCalculator.calculate(
                            sum(accountFacts, PlanningFact::budget),
                            sum(accountFacts, VarianceReportBuilder::actualOrZero),
                            VarianceDirection.forAccountType(first.accountType()));
                    return new AccountVarianceRow(first.accountCode(), first.accountName(), first.accountType(), line);
                })
                .toList();
    }

    private static VarianceSummary summary(List<PlanningFact> facts) {
        List<PlanningFact> revenue = filter(facts, fact -> fact.accountType() == AccountType.REVENUE);
        List<PlanningFact> expenses = filter(facts, fact -> fact.accountType() == AccountType.EXPENSE);
        return new VarianceSummary(
                VarianceCalculator.calculate(
                        sum(revenue, PlanningFact::budget), sum(revenue, VarianceReportBuilder::actualOrZero),
                        VarianceDirection.HIGHER_IS_BETTER),
                VarianceCalculator.calculate(
                        sum(expenses, PlanningFact::budget), sum(expenses, VarianceReportBuilder::actualOrZero),
                        VarianceDirection.LOWER_IS_BETTER),
                netLine(facts));
    }

    /** Revenue minus expenses, where a higher result is better. */
    private static VarianceLine netLine(List<PlanningFact> facts) {
        return VarianceCalculator.calculate(
                sum(facts, VarianceReportBuilder::signedBudget),
                sum(facts, VarianceReportBuilder::signedActual),
                VarianceDirection.HIGHER_IS_BETTER);
    }

    private static BigDecimal signedBudget(PlanningFact fact) {
        return fact.accountType().profitImpact(fact.budget());
    }

    private static BigDecimal signedActual(PlanningFact fact) {
        return fact.accountType().profitImpact(actualOrZero(fact));
    }

    private static BigDecimal actualOrZero(PlanningFact fact) {
        return fact.hasActual() ? fact.actual() : ZERO;
    }

    private static BigDecimal sum(List<PlanningFact> facts, Function<PlanningFact, BigDecimal> amount) {
        return facts.stream().map(amount).reduce(ZERO, BigDecimal::add);
    }

    private static List<PlanningFact> filter(List<PlanningFact> facts, Predicate<PlanningFact> predicate) {
        return facts.stream().filter(predicate).toList();
    }

    /** Groups facts by a code, ordered by that code, so reports always list rows in the same order. */
    private static Map<String, List<PlanningFact>> groupSorted(
            List<PlanningFact> facts, Function<PlanningFact, String> code) {
        return facts.stream()
                .sorted(Comparator.comparing(code))
                .collect(Collectors.groupingBy(code, TreeMap::new, Collectors.toList()));
    }
}
