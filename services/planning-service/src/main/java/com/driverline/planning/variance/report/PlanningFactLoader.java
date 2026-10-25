package com.driverline.planning.variance.report;

import com.driverline.planning.account.Account;
import com.driverline.planning.actual.ActualLine;
import com.driverline.planning.actual.ActualLineRepository;
import com.driverline.planning.budget.BudgetLine;
import com.driverline.planning.budget.BudgetLineRepository;
import com.driverline.planning.common.LineKey;
import com.driverline.planning.department.Department;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a year's budgets and actuals (two queries) and merges them into one fact per account, department and month.
 * A small company has a few hundred lines a year, so the reports aggregate these in memory with exact BigDecimal sums.
 */
@Service
public class PlanningFactLoader {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final BudgetLineRepository budgetLineRepository;
    private final ActualLineRepository actualLineRepository;

    public PlanningFactLoader(BudgetLineRepository budgetLineRepository, ActualLineRepository actualLineRepository) {
        this.budgetLineRepository = budgetLineRepository;
        this.actualLineRepository = actualLineRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanningFact> loadYear(int year) {
        Map<LineKey, BudgetLine> budgets = budgetLineRepository.findByPeriodYear(year).stream()
                .collect(Collectors.toMap(PlanningFactLoader::keyOf, Function.identity()));
        Map<LineKey, ActualLine> actuals = actualLineRepository.findByPeriodYear(year).stream()
                .collect(Collectors.toMap(PlanningFactLoader::keyOf, Function.identity()));

        Set<LineKey> allKeys = new LinkedHashSet<>(budgets.keySet());
        allKeys.addAll(actuals.keySet());

        return allKeys.stream().map(key -> toFact(key, budgets.get(key), actuals.get(key))).toList();
    }

    private static PlanningFact toFact(LineKey key, BudgetLine budget, ActualLine actual) {
        Account account = budget != null ? budget.getAccount() : actual.getAccount();
        Department department = budget != null ? budget.getDepartment() : actual.getDepartment();
        return new PlanningFact(
                account.getCode(),
                account.getName(),
                account.getType(),
                department.getCode(),
                department.getName(),
                key.period().month(),
                budget != null ? budget.getAmount() : ZERO,
                actual != null ? actual.getAmount() : null);
    }

    private static LineKey keyOf(BudgetLine line) {
        return new LineKey(line.getAccount().getCode(), line.getDepartment().getCode(), line.getPeriod());
    }

    private static LineKey keyOf(ActualLine line) {
        return new LineKey(line.getAccount().getCode(), line.getDepartment().getCode(), line.getPeriod());
    }
}
