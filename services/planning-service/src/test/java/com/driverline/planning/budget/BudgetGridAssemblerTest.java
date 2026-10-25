package com.driverline.planning.budget;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;
import com.driverline.planning.budget.dto.BudgetGridRow;
import com.driverline.planning.department.Department;
import com.driverline.planning.period.FiscalPeriod;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetGridAssemblerTest {

    private final BudgetGridAssembler assembler = new BudgetGridAssembler();

    private final Account wages = new Account("5000", "Wages - Cleaning Staff", AccountType.EXPENSE);
    private final Account supplies = new Account("5200", "Cleaning Supplies", AccountType.EXPENSE);
    private final Department ops = new Department("OPS", "Operations");
    private final Department admin = new Department("ADMIN", "Administration");

    private BudgetLine line(Account account, Department department, int month, String amount) {
        return new BudgetLine(account, department, FiscalPeriod.of(2026, month), new BigDecimal(amount));
    }

    @Test
    void pivotsMonthlyLinesIntoOneRowPerAccountAndDepartment() {
        List<BudgetGridRow> rows = assembler.assemble(List.of(
                line(wages, ops, 2, "200.00"),
                line(wages, ops, 1, "100.00"),
                line(supplies, ops, 1, "50.00")));

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().months().subList(0, 3))
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("200.00"), null);
    }

    @Test
    void leavesMonthsWithoutABudgetEmptyAndTotalsOnlyEnteredMonths() {
        BudgetGridRow row = assembler.assemble(List.of(
                line(wages, ops, 3, "1000.10"),
                line(wages, ops, 12, "999.95"))).getFirst();

        assertThat(row.months()).hasSize(12);
        assertThat(row.months().stream().filter(m -> m != null)).hasSize(2);
        assertThat(row.total()).isEqualTo(new BigDecimal("2000.05"));
    }

    @Test
    void ordersRowsByDepartmentCodeThenAccountCode() {
        List<BudgetGridRow> rows = assembler.assemble(List.of(
                line(supplies, ops, 1, "1.00"),
                line(wages, ops, 1, "1.00"),
                line(wages, admin, 1, "1.00")));

        assertThat(rows).extracting(r -> r.departmentCode() + "/" + r.accountCode())
                .containsExactly("ADMIN/5000", "OPS/5000", "OPS/5200");
    }

    @Test
    void returnsNoRowsWhenThereAreNoLines() {
        assertThat(assembler.assemble(List.of())).isEmpty();
    }
}
