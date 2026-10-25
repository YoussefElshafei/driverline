package com.driverline.planning.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.driverline.planning.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against the seeded Ace Janitorial data and its FY2026 demo drivers (Base: 62 contracts x $4,700,
 * 16 jobs x $2,900, supplies 7.6% of revenue, ...). Changes made by a test are rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ForecastControllerIT {

    private static final String SALES_CONTRACTS = "$.departments[?(@.departmentCode == 'SALES')].lines[?(@.accountCode == '4000')]";
    private static final String OPS_SUPPLIES = "$.departments[?(@.departmentCode == 'OPS')].lines[?(@.accountCode == '5200')]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String getJson(String url) throws Exception {
        return mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal firstMoney(String json, String path) {
        List<String> values = JsonPath.read(json, path);
        return new BigDecimal(values.getFirst());
    }

    private BigDecimal sqlActual(String account, String department, int year, int fromMonth, int toMonth) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(l.amount), 0) FROM actual_line l
                JOIN account a ON a.id = l.account_id JOIN department d ON d.id = l.department_id
                WHERE a.code = ? AND d.code = ? AND l.fiscal_year = ? AND l.fiscal_month BETWEEN ? AND ?
                """, BigDecimal.class, account, department, year, fromMonth, toMonth);
    }

    @Nested
    class Forecast {

        @Test
        void closedMonthsAreTheRecordedActualsAndOpenMonthsFollowTheDrivers() throws Exception {
            String json = getJson("/api/forecast?year=2026");

            assertThat(JsonPath.<Integer>read(json, "$.latestActualMonth")).isEqualTo(9);
            assertThat(JsonPath.<String>read(json, "$.scenarioCode")).isEqualTo("BASE");
            assertThat(firstMoney(json, SALES_CONTRACTS + ".months[8].amount"))
                    .isEqualByComparingTo(sqlActual("4000", "SALES", 2026, 9, 9));
            assertThat(JsonPath.<List<Boolean>>read(json, SALES_CONTRACTS + ".months[8].actual")).containsExactly(true);
            // October: 62 active contracts x $4,700
            assertThat(firstMoney(json, SALES_CONTRACTS + ".months[9].amount")).isEqualByComparingTo("291400.00");
            assertThat(JsonPath.<List<Boolean>>read(json, SALES_CONTRACTS + ".months[9].actual")).containsExactly(false);
        }

        @Test
        void fullYearOutlookIsActualsToDatePlusTheForecast() throws Exception {
            String json = getJson("/api/forecast?year=2026");

            BigDecimal expected = sqlActual("4000", "SALES", 2026, 1, 9).add(new BigDecimal("874200.00"));
            assertThat(firstMoney(json, SALES_CONTRACTS + ".outlook.outlook")).isEqualByComparingTo(expected);
        }

        @Test
        void percentOfRevenueLinesFollowTheScenariosRevenue() throws Exception {
            String base = getJson("/api/forecast?year=2026&scenario=BASE");
            String worst = getJson("/api/forecast?year=2026&scenario=WORST");

            // Base October revenue 291,400 + 46,400 = 337,800; supplies 7.6% = 25,672.80
            assertThat(firstMoney(base, OPS_SUPPLIES + ".months[9].amount")).isEqualByComparingTo("25672.80");
            // Worst October revenue 279,000 + 37,050 = 316,050; supplies 8.0% = 25,284.00
            assertThat(firstMoney(worst, OPS_SUPPLIES + ".months[9].amount")).isEqualByComparingTo("25284.00");
        }

        @Test
        void linesWithoutDriversUseTheirBudget() throws Exception {
            String json = getJson("/api/forecast?year=2026");
            BigDecimal octoberRentBudget = jdbcTemplate.queryForObject("""
                    SELECT b.amount FROM budget_line b JOIN account a ON a.id = b.account_id
                    WHERE a.code = '6000' AND b.fiscal_year = 2026 AND b.fiscal_month = 10
                    """, BigDecimal.class);

            String rent = "$.departments[?(@.departmentCode == 'ADMIN')].lines[?(@.accountCode == '6000')]";
            assertThat(JsonPath.<List<String>>read(json, rent + ".method")).containsExactly("BUDGET");
            assertThat(firstMoney(json, rent + ".months[9].amount")).isEqualByComparingTo(octoberRentBudget);
        }

        @Test
        void departmentContributionsAddUpToNetIncomeOutlook() throws Exception {
            String json = getJson("/api/forecast?year=2026");

            List<String> contributions = JsonPath.read(json, "$.departments[*].netContribution.outlook");
            BigDecimal total = contributions.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(total).isEqualByComparingTo(new BigDecimal(JsonPath.<String>read(json, "$.summary.netIncome.outlook")));
        }

        @Test
        void aYearWithNoDataReturnsAnEmptyForecast() throws Exception {
            mockMvc.perform(get("/api/forecast").param("year", "2030"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.latestActualMonth").value(0))
                    .andExpect(jsonPath("$.departments", hasSize(0)));
        }

        @Test
        void rejectsAnUnknownScenario() throws Exception {
            mockMvc.perform(get("/api/forecast").param("year", "2026").param("scenario", "MAYBE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("scenario"));
        }
    }

    @Nested
    class Scenarios {

        @Test
        void listsBaseBestAndWorst() throws Exception {
            mockMvc.perform(get("/api/forecast/scenarios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].code", contains("BASE", "BEST", "WORST")));
        }

        @Test
        void bestBeatsBaseWhichBeatsWorstAgainstTheSameBudget() throws Exception {
            String json = getJson("/api/forecast/compare?year=2026");

            List<String> netOutlooks = JsonPath.read(json, "$.scenarios[*].summary.netIncome.outlook");
            List<String> netBudgets = JsonPath.read(json, "$.scenarios[*].summary.netIncome.budget");
            BigDecimal base = new BigDecimal(netOutlooks.get(0));
            BigDecimal best = new BigDecimal(netOutlooks.get(1));
            BigDecimal worst = new BigDecimal(netOutlooks.get(2));

            assertThat(best).isGreaterThan(base);
            assertThat(base).isGreaterThan(worst);
            assertThat(netBudgets).containsOnly(netBudgets.getFirst());
            assertThat(JsonPath.<List<String>>read(json, "$.scenarios[*].scenarioName")).containsExactly("Base", "Best", "Worst");
        }
    }

    @Nested
    class Drivers {

        @Test
        void listsEveryLineWithItsMethodLabelsAndScenarioValues() throws Exception {
            String json = getJson("/api/forecast/drivers?year=2026");
            String contracts = "$.lines[?(@.accountCode == '4000')]";
            String rent = "$.lines[?(@.accountCode == '6000')]";

            assertThat(JsonPath.<List<Object>>read(json, "$.lines")).hasSize(15);
            assertThat(JsonPath.<List<String>>read(json, contracts + ".quantityLabel")).containsExactly("Active contracts");
            assertThat(firstMoney(json, contracts + ".values.BASE.quantity")).isEqualByComparingTo("62");
            assertThat(firstMoney(json, contracts + ".values.WORST.rate")).isEqualByComparingTo("4650.00");
            assertThat(JsonPath.<List<Boolean>>read(json, rent + ".configured")).containsExactly(false);
            assertThat(firstMoney(json, rent + ".averageOpenMonthBudget")).isEqualByComparingTo("12600.00");
        }

        @Test
        void savingADriverChangesTheForecastImmediately() throws Exception {
            mockMvc.perform(put("/api/forecast/drivers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "lines": [{
                              "accountCode": "5300", "departmentCode": "FLEET", "method": "UNITS_X_RATE",
                              "quantityLabel": "Vans", "rateLabel": "Monthly cost per van",
                              "values": {
                                "BASE":  {"quantity": "15", "rate": "1400.00"},
                                "BEST":  {"quantity": "14", "rate": "1150.00"},
                                "WORST": {"quantity": "14", "rate": "1350.00"}
                              }
                            }]}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linesSaved").value(1));

            String json = getJson("/api/forecast?year=2026");
            String fuel = "$.departments[?(@.departmentCode == 'FLEET')].lines[?(@.accountCode == '5300')]";
            assertThat(firstMoney(json, fuel + ".months[9].amount")).isEqualByComparingTo("21000.00");
        }

        @Test
        void aBudgetLineCanBeSwitchedToDrivers() throws Exception {
            mockMvc.perform(put("/api/forecast/drivers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "lines": [{
                              "accountCode": "6000", "departmentCode": "ADMIN", "method": "UNITS_X_RATE",
                              "values": {
                                "BASE":  {"quantity": "1", "rate": "13000.00"},
                                "BEST":  {"quantity": "1", "rate": "12600.00"},
                                "WORST": {"quantity": "1", "rate": "13500.00"}
                              }
                            }]}
                            """))
                    .andExpect(status().isOk());

            String json = getJson("/api/forecast/drivers?year=2026");
            String rent = "$.lines[?(@.accountCode == '6000')]";
            assertThat(JsonPath.<List<Boolean>>read(json, rent + ".configured")).containsExactly(true);
            assertThat(JsonPath.<List<String>>read(json, rent + ".quantityLabel")).containsExactly("Quantity");
        }

        @Test
        void rejectsARevenueLineAsAPercentageOfRevenueAndSavesNothing() throws Exception {
            mockMvc.perform(put("/api/forecast/drivers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "lines": [{
                              "accountCode": "4000", "departmentCode": "SALES", "method": "PERCENT_OF_REVENUE",
                              "values": {"BASE": {"percent": "5"}, "BEST": {"percent": "5"}, "WORST": {"percent": "5"}}
                            }]}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Some drivers are invalid. Nothing was saved."))
                    .andExpect(jsonPath("$.errors[0].field").value("lines[0].method"));
        }

        @Test
        void reportsMissingAndOutOfRangeValuesByTheirPath() throws Exception {
            mockMvc.perform(put("/api/forecast/drivers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "lines": [{
                              "accountCode": "5300", "departmentCode": "FLEET", "method": "UNITS_X_RATE",
                              "values": {"BASE": {"quantity": "-1", "rate": "1250.00"}}
                            }]}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", hasItem(containsString("BASE"))));

            mockMvc.perform(put("/api/forecast/drivers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "lines": [{
                              "accountCode": "5300", "departmentCode": "FLEET", "method": "UNITS_X_RATE",
                              "values": {"BASE": {"quantity": "14", "rate": "1250.00"}}
                            }]}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", hasItem("lines[0].values.BEST.quantity")))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("lines[0].values.WORST.rate")));
        }
    }

    @Nested
    class Export {

        @Test
        void downloadsAWorkbookWhoseTotalsMatchTheApi() throws Exception {
            String json = getJson("/api/forecast?year=2026&scenario=BEST");
            byte[] bytes = mockMvc.perform(get("/api/forecast/export").param("year", "2026").param("scenario", "BEST"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(ForecastController.XLSX))
                    .andExpect(header().string("Content-Disposition", containsString("forecast-FY2026-best.xlsx")))
                    .andReturn().getResponse().getContentAsByteArray();

            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = workbook.getSheet("Forecast - Best");
                Row netIncome = null;
                for (Row row : sheet) {
                    if (row.getCell(0) != null && "Net income".equals(row.getCell(0).getStringCellValue())) {
                        netIncome = row;
                    }
                }
                assertThat(netIncome).isNotNull();
                BigDecimal workbookOutlook = BigDecimal.valueOf(netIncome.getCell(13).getNumericCellValue())
                        .setScale(2, RoundingMode.HALF_UP);
                assertThat(workbookOutlook)
                        .isEqualByComparingTo(new BigDecimal(JsonPath.<String>read(json, "$.summary.netIncome.outlook")));
                assertThat(workbook.getSheet("Scenarios")).isNotNull();
            }
        }
    }
}
