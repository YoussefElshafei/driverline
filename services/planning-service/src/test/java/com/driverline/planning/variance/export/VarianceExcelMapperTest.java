package com.driverline.planning.variance.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.variance.VarianceCalculator;
import com.driverline.planning.variance.VarianceDirection;
import com.driverline.planning.variance.VarianceLine;
import com.driverline.planning.variance.dto.AccountVarianceRow;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceRow;
import com.driverline.planning.variance.dto.MonthlyVarianceReport;
import com.driverline.planning.variance.dto.MonthlyVarianceRow;
import com.driverline.planning.variance.dto.VarianceSummary;
import com.driverline.planning.variance.export.ExcelRow.Kind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VarianceExcelMapperTest {

    private final VarianceExcelMapper mapper = new VarianceExcelMapper();
    private final VarianceLine line = VarianceCalculator.calculate(
            new BigDecimal("100.00"), new BigDecimal("90.00"), VarianceDirection.LOWER_IS_BETTER);

    @Test
    void putsEachDepartmentAboveItsIndentedAccountsThenCompanyTotals() {
        var report = new DepartmentVarianceReport(2026, 1, 9, 9, new VarianceSummary(line, line, line), List.of(
                new DepartmentVarianceRow("OPS", "Operations", line, List.of(
                        new AccountVarianceRow("5200", "Cleaning Supplies", AccountType.EXPENSE, line)))));

        ExcelReport excel = mapper.byDepartment(report);

        assertThat(excel.fileName()).isEqualTo("variance-by-department-FY2026-Jan-Sep.xlsx");
        assertThat(excel.rows()).extracting(ExcelRow::label, ExcelRow::kind, ExcelRow::outlineLevel).containsExactly(
                tuple("Operations – net contribution", Kind.SUBTOTAL, 0),
                tuple("5200 Cleaning Supplies", Kind.DETAIL, 1),
                tuple("Company totals", Kind.SECTION, 0),
                tuple("Total revenue", Kind.SUBTOTAL, 0),
                tuple("Total expenses", Kind.SUBTOTAL, 0),
                tuple("Net income", Kind.TOTAL, 0));
        assertThat(excel.subtitles()).contains("FY2026, January to September", "Actuals recorded through September.");
    }

    @Test
    void marksMonthsWithoutActualsAsNotClosed() {
        List<MonthlyVarianceRow> months = new ArrayList<>();
        months.add(new MonthlyVarianceRow(1, line.budget(), true, line));
        for (int month = 2; month <= 12; month++) {
            months.add(new MonthlyVarianceRow(month, new BigDecimal("100.00"), false, null));
        }
        var report = new MonthlyVarianceReport(2026, "FLEET", "5300", "5300 Vehicle & Fuel – Fleet & Equipment",
                VarianceDirection.LOWER_IS_BETTER, 1, months, line);

        ExcelReport excel = mapper.byMonth(report);

        assertThat(excel.fileName()).isEqualTo("variance-by-month-FY2026-fleet-5300.xlsx");
        assertThat(excel.rows().get(0).actual()).isEqualTo(new BigDecimal("90.00"));
        assertThat(excel.rows().get(1).label()).isEqualTo("February");
        assertThat(excel.rows().get(1).actual()).isNull();
        assertThat(excel.rows().getLast().label()).isEqualTo("Closed months total");
    }
}
