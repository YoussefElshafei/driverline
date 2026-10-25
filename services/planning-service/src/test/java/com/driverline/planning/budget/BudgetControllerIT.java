package com.driverline.planning.budget;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.driverline.planning.TestcontainersConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs against the seeded Ace Janitorial demo data. Each test's changes are rolled back (@Transactional). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class BudgetControllerIT {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    class GetGrid {

        @Test
        void returnsOneRowPerAccountInTheDepartmentWithMoneyAsStrings() throws Exception {
            mockMvc.perform(get("/api/budgets").param("year", "2025").param("department", "SALES"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.year").value(2025))
                    .andExpect(jsonPath("$.departmentCode").value("SALES"))
                    .andExpect(jsonPath("$.rows", hasSize(4)))
                    .andExpect(jsonPath("$.rows[0].accountCode").value("4000"))
                    .andExpect(jsonPath("$.rows[0].accountType").value("REVENUE"))
                    .andExpect(jsonPath("$.rows[0].months", hasSize(12)))
                    // 290000.00 x 0.99 (January contract seasonality), sent as a string so no precision is lost in JavaScript.
                    .andExpect(jsonPath("$.rows[0].months[0]").value("287100.00"))
                    .andExpect(jsonPath("$.rows[0].total").isString());
        }

        @Test
        void returnsEveryDepartmentWhenNoFilterIsGiven() throws Exception {
            mockMvc.perform(get("/api/budgets").param("year", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.departmentCode").value(nullValue()))
                    .andExpect(jsonPath("$.rows", hasSize(15)));
        }

        @Test
        void returnsAnEmptyGridForAYearWithNoBudget() throws Exception {
            mockMvc.perform(get("/api/budgets").param("year", "2030"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows", hasSize(0)));
        }

        @Test
        void rejectsAnUnknownDepartmentWithAProblemDetail() throws Exception {
            mockMvc.perform(get("/api/budgets").param("year", "2026").param("department", "NOPE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.errors[0].field").value("department"))
                    .andExpect(jsonPath("$.errors[0].message").value("Unknown department code 'NOPE'"));
        }

        @Test
        void rejectsAYearOutsideTheSupportedRange() throws Exception {
            mockMvc.perform(get("/api/budgets").param("year", "1999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("year"));
        }

        @Test
        void rejectsAMissingYear() throws Exception {
            mockMvc.perform(get("/api/budgets"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
        }
    }

    @Nested
    class Save {

        @Test
        void updatesAnExistingCellAndCreatesANewRowThenTheGridShowsBoth() throws Exception {
            // OPS has no Marketing budget in the demo data, so that entry creates a new row.
            mockMvc.perform(put("/api/budgets").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "entries": [
                              {"accountCode": "5200", "departmentCode": "OPS", "month": 1, "amount": "23000.00"},
                              {"accountCode": "6100", "departmentCode": "OPS", "month": 2, "amount": 1500.5}
                            ]}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.created").value(1))
                    .andExpect(jsonPath("$.updated").value(1));

            mockMvc.perform(get("/api/budgets").param("year", "2026").param("department", "OPS"))
                    .andExpect(jsonPath("$.rows", hasSize(4)))
                    .andExpect(jsonPath("$.rows[?(@.accountCode == '5200')].months[0]", hasItem("23000.00")))
                    .andExpect(jsonPath("$.rows[?(@.accountCode == '6100')].months[1]", hasItem("1500.50")))
                    .andExpect(jsonPath("$.rows[?(@.accountCode == '6100')].months[0]", hasItem(nullValue())));
        }

        @Test
        void reportsEachInvalidFieldByItsPath() throws Exception {
            mockMvc.perform(put("/api/budgets").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "entries": [
                              {"accountCode": "5200", "departmentCode": "OPS", "month": 13, "amount": "12.345"}
                            ]}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("entries[0].month")))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("entries[0].amount")));
        }

        @Test
        void savesNothingWhenOneEntryHasAnUnknownAccount() throws Exception {
            mockMvc.perform(put("/api/budgets").contentType(MediaType.APPLICATION_JSON).content("""
                            {"year": 2026, "entries": [
                              {"accountCode": "5200", "departmentCode": "OPS", "month": 1, "amount": "1.00"},
                              {"accountCode": "9999", "departmentCode": "OPS", "month": 1, "amount": "1.00"}
                            ]}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Some budget entries are invalid. Nothing was saved."))
                    .andExpect(jsonPath("$.errors[0].field").value("entries[1].accountCode"));

            mockMvc.perform(get("/api/budgets").param("year", "2026").param("department", "OPS"))
                    .andExpect(jsonPath("$.rows[?(@.accountCode == '5200')].months[0]", hasItem("23940.00")));
        }

        @Test
        void returnsAProblemDetailForMalformedJson() throws Exception {
            mockMvc.perform(put("/api/budgets").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
