package com.driverline.planning.commentary;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.commentary.dto.CommentaryRequest.Highlight;
import com.driverline.planning.variance.VarianceCalculator;
import com.driverline.planning.variance.VarianceDirection;
import com.driverline.planning.variance.VarianceLine;
import com.driverline.planning.variance.dto.AccountVarianceRow;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceRow;
import com.driverline.planning.variance.dto.VarianceSummary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class VarianceHighlightSelectorTest {

    private final VarianceHighlightSelector selector = new VarianceHighlightSelector();

    private static VarianceLine line(String budget, String actual, VarianceDirection direction) {
        return VarianceCalculator.calculate(new BigDecimal(budget), new BigDecimal(actual), direction);
    }

    private static AccountVarianceRow expense(String code, String name, String budget, String actual) {
        return new AccountVarianceRow(code, name, AccountType.EXPENSE, line(budget, actual, VarianceDirection.LOWER_IS_BETTER));
    }

    private static DepartmentVarianceReport report(DepartmentVarianceRow... departments) {
        VarianceLine zero = line("0.00", "0.00", VarianceDirection.HIGHER_IS_BETTER);
        return new DepartmentVarianceReport(2026, 1, 9, 9, new VarianceSummary(zero, zero, zero), List.of(departments));
    }

    private static DepartmentVarianceRow department(String code, String name, AccountVarianceRow... accounts) {
        return new DepartmentVarianceRow(code, name, line("0.00", "0.00", VarianceDirection.HIGHER_IS_BETTER), List.of(accounts));
    }

    @Test
    void picksTheLargestDollarVariancesWhetherFavorableOrNot() {
        DepartmentVarianceReport report = report(
                department("OPS", "Operations",
                        expense("5000", "Wages", "1000000.00", "990000.00"),
                        expense("5200", "Supplies", "200000.00", "216000.00")),
                department("FLEET", "Fleet & Equipment",
                        expense("5300", "Vehicle & Fuel", "100000.00", "114000.00"),
                        expense("5400", "Equipment", "100.00", "200.00")));

        List<Highlight> highlights = selector.select(report, 3);

        assertThat(highlights).extracting(Highlight::accountName).containsExactly("Supplies", "Vehicle & Fuel", "Wages");
        assertThat(highlights.getFirst().departmentName()).isEqualTo("Operations");
        assertThat(highlights.getFirst().figures().variance()).isEqualByComparingTo("16000.00");
    }

    @Test
    void skipsLinesThatAreExactlyOnBudget() {
        DepartmentVarianceReport report = report(department("ADMIN", "Administration",
                expense("6000", "Rent", "12600.00", "12600.00"),
                expense("6300", "Software", "3360.00", "3400.00")));

        assertThat(selector.select(report, 5)).extracting(Highlight::accountName).containsExactly("Software");
    }

    @Test
    void ordersEqualVariancesByDepartmentThenAccountSoTheResultIsStable() {
        DepartmentVarianceReport report = report(
                department("SALES", "Sales", expense("6100", "Marketing", "100.00", "150.00")),
                department("ADMIN", "Administration", expense("6300", "Software", "100.00", "150.00")));

        assertThat(selector.select(report, 5)).extracting(Highlight::accountName).containsExactly("Software", "Marketing");
    }
}
