package com.driverline.planning.actual.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.driverline.planning.TestcontainersConfiguration;
import com.driverline.planning.actual.ActualLine;
import com.driverline.planning.actual.ActualLineRepository;
import com.driverline.planning.period.FiscalPeriod;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs against the seeded demo data (actuals exist through September 2026). Each test is rolled back. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ActualsImportControllerIT {

    private static final String HEADER = "year,month,account_code,department_code,amount\n";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActualLineRepository actualLineRepository;

    private static MockMultipartFile csvFile(String body) {
        return new MockMultipartFile("file", "actuals.csv", "text/csv", (HEADER + body).getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal storedActual(String accountCode, String departmentCode, int year, int month) {
        return actualLineRepository.findByPeriodYear(year).stream()
                .filter(line -> line.getAccount().getCode().equals(accountCode))
                .filter(line -> line.getDepartment().getCode().equals(departmentCode))
                .filter(line -> line.getPeriod().equals(FiscalPeriod.of(year, month)))
                .map(ActualLine::getAmount)
                .findFirst()
                .orElse(null);
    }

    @Test
    void importsANewMonthFromCsv() throws Exception {
        mockMvc.perform(multipart("/api/actuals/import").file(csvFile("""
                        2026,10,4000,SALES,"301,250.00"
                        2026,10,5300,FLEET,17890.45
                        """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("actuals.csv"))
                .andExpect(jsonPath("$.rowsImported").value(2))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.periods[0]").value("2026-10"));

        assertThat(storedActual("4000", "SALES", 2026, 10)).isEqualTo(new BigDecimal("301250.00"));
    }

    @Test
    void reimportingAMonthReplacesAmountsInsteadOfDuplicating() throws Exception {
        long before = actualLineRepository.count();

        mockMvc.perform(multipart("/api/actuals/import").file(csvFile("2026,9,5300,FLEET,20000.00\n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(1));

        assertThat(actualLineRepository.count()).isEqualTo(before);
        assertThat(storedActual("5300", "FLEET", 2026, 9)).isEqualTo(new BigDecimal("20000.00"));
    }

    @Test
    void importsFromAnExcelWorkbook() throws Exception {
        byte[] workbook = XlsxTestFiles.workbook(List.of(
                List.of("year", "month", "account_code", "department_code", "amount"),
                List.of(2026, 10, 5300, "FLEET", 17890.45)));
        var file = new MockMultipartFile("file", "actuals.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook);

        mockMvc.perform(multipart("/api/actuals/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1));

        assertThat(storedActual("5300", "FLEET", 2026, 10)).isEqualTo(new BigDecimal("17890.45"));
    }

    @Test
    void rejectsTheWholeFileAndListsEveryBadRow() throws Exception {
        long before = actualLineRepository.count();

        mockMvc.perform(multipart("/api/actuals/import").file(csvFile("""
                        2026,10,4000,SALES,100.00
                        2026,13,4000,SALES,100.00
                        2026,10,9999,SALES,12.345
                        """)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Import failed"))
                .andExpect(jsonPath("$.detail").value("Found 3 problems in the file. No rows were imported."))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[0].row").value(3))
                .andExpect(jsonPath("$.errors[0].column").value("month"))
                .andExpect(jsonPath("$.errors[0].value").value("13"))
                .andExpect(jsonPath("$.errors[1].row").value(4))
                .andExpect(jsonPath("$.errors[1].column").value("account_code"));

        // Row 2 was valid, but all-or-nothing means it was not saved either.
        assertThat(actualLineRepository.count()).isEqualTo(before);
    }

    @Test
    void rejectsUnsupportedFileTypes() throws Exception {
        var file = new MockMultipartFile("file", "actuals.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/actuals/import").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail", containsString("Upload a .csv or .xlsx file")));
    }

    @Test
    void rejectsARequestWithNoFile() throws Exception {
        mockMvc.perform(multipart("/api/actuals/import"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void servesADownloadableCsvTemplate() throws Exception {
        mockMvc.perform(get("/api/actuals/import/template"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("actuals-template.csv")))
                .andExpect(content().string(containsString("year,month,account_code,department_code,amount")));
    }
}
