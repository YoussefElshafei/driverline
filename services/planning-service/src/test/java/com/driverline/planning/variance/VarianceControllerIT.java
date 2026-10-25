package com.driverline.planning.variance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.driverline.planning.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs the reports against the seeded Ace Janitorial data. Totals are checked against independent SQL SUMs,
 * so the Java aggregation is verified by a completely different calculation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VarianceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BigDecimal sqlSum(String table, String accountType, int year, int toMonth) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(l.amount), 0)
                FROM %s l JOIN account a ON a.id = l.account_id
                WHERE a.type = ? AND l.fiscal_year = ? AND l.fiscal_month <= ?
                """.formatted(table), BigDecimal.class, accountType, year, toMonth);
    }

    private String getJson(String url) throws Exception {
        return mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal money(String json, String path) {
        return new BigDecimal(JsonPath.<String>read(json, path));
    }

    @Nested
    class ByDepartment {

        @Test
        void fullYearTotalsMatchIndependentSqlSums() throws Exception {
            String json = getJson("/api/variance/by-department?year=2025");

            BigDecimal revenueBudget = sqlSum("budget_line", "REVENUE", 2025, 12);
            BigDecimal revenueActual = sqlSum("actual_line", "REVENUE", 2025, 12);
            BigDecimal expenseBudget = sqlSum("budget_line", "EXPENSE", 2025, 12);
            BigDecimal expenseActual = sqlSum("actual_line", "EXPENSE", 2025, 12);

            assertThat(money(json, "$.summary.revenue.budget")).isEqualByComparingTo(revenueBudget);
            assertThat(money(json, "$.summary.revenue.actual")).isEqualByComparingTo(revenueActual);
            assertThat(money(json, "$.summary.expenses.actual")).isEqualByComparingTo(expenseActual);
            assertThat(money(json, "$.summary.netIncome.budget")).isEqualByComparingTo(revenueBudget.subtract(expenseBudget));
            assertThat(money(json, "$.summary.netIncome.actual")).isEqualByComparingTo(revenueActual.subtract(expenseActual));
        }

        @Test
        void departmentContributionsAddUpToCompanyNetIncome() throws Exception {
            String json = getJson("/api/variance/by-department?year=2025");

            List<String> contributions = JsonPath.read(json, "$.departments[*].netContribution.actual");
            BigDecimal total = contributions.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(contributions).hasSize(4);
            assertThat(total).isEqualByComparingTo(money(json, "$.summary.netIncome.actual"));
        }

        @Test
        void defaultsToJanuaryThroughTheLatestClosedMonthAndNestsAccountsForDrillDown() throws Exception {
            mockMvc.perform(get("/api/variance/by-department").param("year", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromMonth").value(1))
                    .andExpect(jsonPath("$.toMonth").value(9))
                    .andExpect(jsonPath("$.latestActualMonth").value(9))
                    .andExpect(jsonPath("$.departments[*].departmentCode")
                            .value(contains("ADMIN", "FLEET", "OPS", "SALES")))
                    .andExpect(jsonPath("$.departments[1].accounts", hasSize(3)))
                    .andExpect(jsonPath("$.departments[1].accounts[*].accountCode")
                            .value(contains("5100", "5300", "5400")));
        }

        @Test
        void theFuelPriceSpikeShowsAsAnUnfavorableVehicleVariance() throws Exception {
            mockMvc.perform(get("/api/variance/by-department").param("year", "2026")
                            .param("fromMonth", "4").param("toMonth", "9"))
                    .andExpect(jsonPath("$.departments[1].accounts[1].accountCode").value("5300"))
                    .andExpect(jsonPath("$.departments[1].accounts[1].line.favorability").value("UNFAVORABLE"));
        }

        @Test
        void sendsMoneyAndPercentagesAsStrings() throws Exception {
            mockMvc.perform(get("/api/variance/by-department").param("year", "2025"))
                    .andExpect(jsonPath("$.summary.revenue.budget").isString())
                    .andExpect(jsonPath("$.summary.revenue.variancePercent").isString());
        }

        @Test
        void aYearWithNoDataReturnsAnEmptyReportOverTheWholeYear() throws Exception {
            mockMvc.perform(get("/api/variance/by-department").param("year", "2030"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.latestActualMonth").value(0))
                    .andExpect(jsonPath("$.toMonth").value(12))
                    .andExpect(jsonPath("$.departments", hasSize(0)))
                    .andExpect(jsonPath("$.summary.netIncome.variancePercent").value(nullValue()))
                    .andExpect(jsonPath("$.summary.netIncome.favorability").value("ON_BUDGET"));
        }

        @Test
        void rejectsAMonthRangeThatEndsBeforeItStarts() throws Exception {
            mockMvc.perform(get("/api/variance/by-department").param("year", "2026")
                            .param("fromMonth", "10").param("toMonth", "3"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("toMonth"));
        }

        @Test
        void rejectsAMonthOutsideOneToTwelve() throws Exception {
            mockMvc.perform(get("/api/variance/by-department").param("year", "2026").param("fromMonth", "13"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("fromMonth"));
        }
    }

    @Nested
    class ByAccount {

        @Test
        void listsRevenueAndExpenseAccountsCombinedAcrossDepartments() throws Exception {
            String json = getJson("/api/variance/by-account?year=2025");

            List<String> revenueCodes = JsonPath.read(json, "$.revenue[*].accountCode");
            List<String> expenseCodes = JsonPath.read(json, "$.expenses[*].accountCode");
            assertThat(revenueCodes).containsExactly("4000", "4100");
            assertThat(expenseCodes).hasSize(10).contains("5100");

            BigDecimal salariesBudget = jdbcTemplate.queryForObject("""
                    SELECT SUM(b.amount) FROM budget_line b JOIN account a ON a.id = b.account_id
                    WHERE a.code = '5100' AND b.fiscal_year = 2025
                    """, BigDecimal.class);
            // Salaries are budgeted in all four departments; the by-account row must combine them.
            List<String> salaries = JsonPath.read(json, "$.expenses[?(@.accountCode == '5100')].line.budget");
            assertThat(new BigDecimal(salaries.getFirst())).isEqualByComparingTo(salariesBudget);
        }
    }

    @Nested
    class ByMonth {

        @Test
        void vehicleCostsAreUnfavorableEveryMonthOfTheFuelSpikeAndLaterMonthsAreNotClosed() throws Exception {
            mockMvc.perform(get("/api/variance/by-month").param("year", "2026")
                            .param("department", "FLEET").param("account", "5300"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scope").value("5300 Vehicle & Fuel – Fleet & Equipment"))
                    .andExpect(jsonPath("$.direction").value("LOWER_IS_BETTER"))
                    .andExpect(jsonPath("$.months", hasSize(12)))
                    .andExpect(jsonPath("$.months[3:9].variance.favorability", everyItem(is("UNFAVORABLE"))))
                    .andExpect(jsonPath("$.months[9:12].closed", everyItem(is(false))))
                    .andExpect(jsonPath("$.months[9].variance").value(nullValue()))
                    .andExpect(jsonPath("$.months[9].budget").isString());
        }

        @Test
        void theLostContractMakesContractRevenueUnfavorableFromJuly() throws Exception {
            mockMvc.perform(get("/api/variance/by-month").param("year", "2026").param("account", "4000"))
                    .andExpect(jsonPath("$.direction").value("HIGHER_IS_BETTER"))
                    .andExpect(jsonPath("$.months[6:9].variance.favorability", everyItem(is("UNFAVORABLE"))));
        }

        @Test
        void withoutAnAccountTheTrendIsNetIncome() throws Exception {
            String json = getJson("/api/variance/by-month?year=2025");
            String summary = getJson("/api/variance/by-department?year=2025");

            assertThat(JsonPath.<String>read(json, "$.scope")).isEqualTo("Net income – all departments");
            assertThat(money(json, "$.closedMonthsTotal.actual"))
                    .isEqualByComparingTo(money(summary, "$.summary.netIncome.actual"));
        }

        @Test
        void rejectsUnknownFilters() throws Exception {
            mockMvc.perform(get("/api/variance/by-month").param("year", "2026")
                            .param("department", "NOPE").param("account", "9999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("department"))
                    .andExpect(jsonPath("$.errors[1].field").value("account"));
        }
    }

    @Nested
    class ExcelExport {

        private XSSFWorkbook download(String url, String expectedFileName) throws Exception {
            byte[] bytes = mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(VarianceController.XLSX))
                    .andExpect(header().string("Content-Disposition",
                            containsString(expectedFileName)))
                    .andReturn().getResponse().getContentAsByteArray();
            return new XSSFWorkbook(new ByteArrayInputStream(bytes));
        }

        private static Row findRow(Sheet sheet, String label) {
            for (Row row : sheet) {
                if (row.getCell(0) != null && label.equals(row.getCell(0).getStringCellValue())) {
                    return row;
                }
            }
            throw new AssertionError("No row labelled " + label);
        }

        @Test
        void byDepartmentWorkbookHasGroupedAccountsAndMatchingTotals() throws Exception {
            String json = getJson("/api/variance/by-department?year=2025");

            try (XSSFWorkbook workbook = download("/api/variance/by-department/export?year=2025",
                    "variance-by-department-FY2025-Jan-Dec.xlsx")) {
                Sheet sheet = workbook.getSheet("By department");
                Row netIncome = findRow(sheet, "Net income");
                Row fleet = findRow(sheet, "Fleet & Equipment – net contribution");
                Row fuel = findRow(sheet, "5300 Vehicle & Fuel");

                assertThat(BigDecimal.valueOf(netIncome.getCell(1).getNumericCellValue()))
                        .isEqualByComparingTo(money(json, "$.summary.netIncome.budget"));
                assertThat(BigDecimal.valueOf(netIncome.getCell(3).getNumericCellValue()))
                        .isEqualByComparingTo(money(json, "$.summary.netIncome.variance"));
                assertThat(fleet.getOutlineLevel()).isZero();
                assertThat(fuel.getOutlineLevel()).isEqualTo(1);
            }
        }

        @Test
        void byAccountWorkbookDownloads() throws Exception {
            try (XSSFWorkbook workbook = download("/api/variance/by-account/export?year=2026&fromMonth=1&toMonth=6",
                    "variance-by-account-FY2026-Jan-Jun.xlsx")) {
                assertThat(findRow(workbook.getSheet("By account"), "Total revenue")).isNotNull();
            }
        }

        @Test
        void byMonthWorkbookMarksOpenMonths() throws Exception {
            try (XSSFWorkbook workbook = download("/api/variance/by-month/export?year=2026&department=OPS",
                    "variance-by-month-FY2026-ops.xlsx")) {
                Row december = findRow(workbook.getSheet("By month"), "December");
                assertThat(december.getCell(5).getStringCellValue()).isEqualTo("Not closed");
            }
        }
    }
}
