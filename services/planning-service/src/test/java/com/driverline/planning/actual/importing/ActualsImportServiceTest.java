package com.driverline.planning.actual.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;
import com.driverline.planning.actual.ActualLine;
import com.driverline.planning.actual.ActualLineRepository;
import com.driverline.planning.department.Department;
import com.driverline.planning.period.FiscalPeriod;
import com.driverline.planning.reference.ReferenceData;
import com.driverline.planning.reference.ReferenceDataService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test with Mockito: the database is replaced by mocks so we can check exactly what would be saved. */
@ExtendWith(MockitoExtension.class)
class ActualsImportServiceTest {

    private static final String HEADER = "year,month,account_code,department_code,amount\n";

    private final Account fuel = new Account("5300", "Vehicle & Fuel", AccountType.EXPENSE);
    private final Account revenue = new Account("4000", "Contract Cleaning Revenue", AccountType.REVENUE);
    private final Department fleet = new Department("FLEET", "Fleet & Equipment");
    private final Department sales = new Department("SALES", "Sales & Account Management");

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ActualLineRepository actualLineRepository;

    @Captor
    private ArgumentCaptor<List<ActualLine>> savedLines;

    private ActualsImportService service;

    @BeforeEach
    void setUp() {
        service = new ActualsImportService(
                List.of(new CsvActualsParser(), new XlsxActualsParser()),
                new ActualRowValidator(),
                referenceDataService,
                actualLineRepository);
    }

    private void givenReferenceData() {
        when(referenceDataService.load()).thenReturn(new ReferenceData(
                Map.of("5300", fuel, "4000", revenue), Map.of("FLEET", fleet, "SALES", sales)));
    }

    private static InputStream csv(String body) {
        return new ByteArrayInputStream((HEADER + body).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void savesNothingWhenAnyRowIsInvalid() {
        givenReferenceData();

        assertThatThrownBy(() -> service.importFile("actuals.csv", csv("""
                2026,10,5300,FLEET,100.00
                2026,10,4000,SALES,not-money
                """)))
                .isInstanceOfSatisfying(ImportValidationException.class, e -> {
                    assertThat(e.getErrors()).singleElement().extracting(RowError::row).isEqualTo(3);
                    assertThat(e.getMessage()).isEqualTo("Found 1 problem in the file. No rows were imported.");
                });

        verify(actualLineRepository, never()).saveAll(any());
    }

    @Test
    void createsNewLinesAndUpdatesExistingOnesInsteadOfDuplicating() {
        givenReferenceData();
        ActualLine existingFuel = new ActualLine(fuel, fleet, FiscalPeriod.of(2026, 10), new BigDecimal("100.00"));
        when(actualLineRepository.findByPeriodYear(2026)).thenReturn(List.of(existingFuel));

        ImportSummary summary = service.importFile("actuals.csv", csv("""
                2026,10,5300,FLEET,175.50
                2026,10,4000,SALES,"301,250.00"
                """));

        assertThat(existingFuel.getAmount()).isEqualTo(new BigDecimal("175.50"));
        verify(actualLineRepository).saveAll(savedLines.capture());
        assertThat(savedLines.getValue()).singleElement().satisfies(line -> {
            assertThat(line.getAccount()).isSameAs(revenue);
            assertThat(line.getAmount()).isEqualTo(new BigDecimal("301250.00"));
        });
        assertThat(summary).isEqualTo(new ImportSummary("actuals.csv", 2, 1, 1, List.of("2026-10")));
    }

    @Test
    void rejectsUnsupportedFileTypesBeforeReadingAnything() {
        assertThatThrownBy(() -> service.importFile("actuals.txt", csv("")))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("actuals.txt");

        verify(actualLineRepository, never()).findByPeriodYear(anyInt());
    }

    @Test
    void rejectsAFileWithAHeaderButNoData() {
        assertThatThrownBy(() -> service.importFile("actuals.csv", csv("")))
                .isInstanceOf(ImportValidationException.class)
                .satisfies(e -> assertThat(((ImportValidationException) e).getErrors().getFirst().message())
                        .contains("no data rows"));
    }

    @Test
    void reportsEveryPeriodTheFileTouchedInOrder() {
        givenReferenceData();

        ImportSummary summary = service.importFile("actuals.csv", csv("""
                2026,11,5300,FLEET,1.00
                2025,12,5300,FLEET,1.00
                2026,10,5300,FLEET,1.00
                """));

        assertThat(summary.periods()).containsExactly("2025-12", "2026-10", "2026-11");
    }
}
