package com.driverline.planning.variance.export;

import static com.driverline.planning.variance.export.ExcelRow.Kind.DETAIL;
import static com.driverline.planning.variance.export.ExcelRow.Kind.SUBTOTAL;
import static com.driverline.planning.variance.export.ExcelRow.Kind.TOTAL;

import com.driverline.planning.variance.dto.AccountVarianceReport;
import com.driverline.planning.variance.dto.AccountVarianceRow;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceRow;
import com.driverline.planning.variance.dto.MonthlyVarianceReport;
import com.driverline.planning.variance.dto.MonthlyVarianceRow;
import com.driverline.planning.variance.dto.VarianceSummary;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Lays out each variance report as spreadsheet rows. */
@Component
public class VarianceExcelMapper {

    public ExcelReport byDepartment(DepartmentVarianceReport report) {
        List<ExcelRow> rows = new ArrayList<>();
        for (DepartmentVarianceRow department : report.departments()) {
            rows.add(ExcelRow.of(department.departmentName() + " – net contribution", SUBTOTAL, 0, department.netContribution()));
            for (AccountVarianceRow account : department.accounts()) {
                rows.add(ExcelRow.of(accountLabel(account), DETAIL, 1, account.line()));
            }
        }
        rows.add(ExcelRow.section("Company totals"));
        addSummary(rows, report.summary());

        return new ExcelReport(
                "variance-by-department-%s.xlsx".formatted(periodSlug(report.year(), report.fromMonth(), report.toMonth())),
                "By department",
                "Budget vs. actual by department",
                List.of(
                        periodText(report.year(), report.fromMonth(), report.toMonth()),
                        actualsText(report.latestActualMonth()),
                        "Department rows show net contribution (revenue minus expenses). Use the + buttons to see each department's accounts."),
                "Department / account",
                rows);
    }

    public ExcelReport byAccount(AccountVarianceReport report) {
        List<ExcelRow> rows = new ArrayList<>();
        rows.add(ExcelRow.section("Revenue"));
        report.revenue().forEach(account -> rows.add(ExcelRow.of(accountLabel(account), DETAIL, 0, account.line())));
        rows.add(ExcelRow.of("Total revenue", SUBTOTAL, 0, report.summary().revenue()));
        rows.add(ExcelRow.section("Expenses"));
        report.expenses().forEach(account -> rows.add(ExcelRow.of(accountLabel(account), DETAIL, 0, account.line())));
        rows.add(ExcelRow.of("Total expenses", SUBTOTAL, 0, report.summary().expenses()));
        rows.add(ExcelRow.of("Net income", TOTAL, 0, report.summary().netIncome()));

        return new ExcelReport(
                "variance-by-account-%s.xlsx".formatted(periodSlug(report.year(), report.fromMonth(), report.toMonth())),
                "By account",
                "Budget vs. actual by account",
                List.of(
                        periodText(report.year(), report.fromMonth(), report.toMonth()),
                        actualsText(report.latestActualMonth()),
                        "Accounts are combined across all departments."),
                "Account",
                rows);
    }

    public ExcelReport byMonth(MonthlyVarianceReport report) {
        List<ExcelRow> rows = new ArrayList<>();
        for (MonthlyVarianceRow month : report.months()) {
            String label = monthName(month.month());
            rows.add(month.closed()
                    ? ExcelRow.of(label, DETAIL, 0, month.variance())
                    : ExcelRow.notClosed(label, month.budget()));
        }
        rows.add(ExcelRow.of("Closed months total", TOTAL, 0, report.closedMonthsTotal()));

        String scopeSlug = (report.departmentCode() == null ? "all" : report.departmentCode().toLowerCase(Locale.ROOT))
                + (report.accountCode() == null ? "" : "-" + report.accountCode());
        return new ExcelReport(
                "variance-by-month-FY%d-%s.xlsx".formatted(report.year(), scopeSlug),
                "By month",
                "Monthly budget vs. actual",
                List.of("FY%d – %s".formatted(report.year(), report.scope()), actualsText(report.latestActualMonth())),
                "Month",
                rows);
    }

    private static void addSummary(List<ExcelRow> rows, VarianceSummary summary) {
        rows.add(ExcelRow.of("Total revenue", SUBTOTAL, 0, summary.revenue()));
        rows.add(ExcelRow.of("Total expenses", SUBTOTAL, 0, summary.expenses()));
        rows.add(ExcelRow.of("Net income", TOTAL, 0, summary.netIncome()));
    }

    private static String accountLabel(AccountVarianceRow account) {
        return account.accountCode() + " " + account.accountName();
    }

    private static String periodText(int year, int fromMonth, int toMonth) {
        return fromMonth == toMonth
                ? "FY%d, %s".formatted(year, monthName(fromMonth))
                : "FY%d, %s to %s".formatted(year, monthName(fromMonth), monthName(toMonth));
    }

    private static String actualsText(int latestActualMonth) {
        return latestActualMonth == 0
                ? "No actuals recorded for this year yet."
                : "Actuals recorded through " + monthName(latestActualMonth) + ".";
    }

    private static String periodSlug(int year, int fromMonth, int toMonth) {
        return "FY%d-%s-%s".formatted(year, shortMonth(fromMonth), shortMonth(toMonth));
    }

    private static String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private static String shortMonth(int month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }
}
