package com.driverline.planning.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;
import com.driverline.planning.budget.dto.BudgetEntryRequest;
import com.driverline.planning.budget.dto.SaveBudgetRequest;
import com.driverline.planning.budget.dto.SaveBudgetResponse;
import com.driverline.planning.common.error.FieldErrorDetail;
import com.driverline.planning.common.error.RequestValidationException;
import com.driverline.planning.department.Department;
import com.driverline.planning.period.FiscalPeriod;
import com.driverline.planning.reference.ReferenceData;
import com.driverline.planning.reference.ReferenceDataService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private final Account marketing = new Account("6100", "Marketing", AccountType.EXPENSE);
    private final Department sales = new Department("SALES", "Sales & Account Management");

    @Mock
    private BudgetLineRepository budgetLineRepository;

    @Mock
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<List<BudgetLine>> savedLines;

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgetLineRepository, referenceDataService, new BudgetGridAssembler());
        when(referenceDataService.load()).thenReturn(new ReferenceData(Map.of("6100", marketing), Map.of("SALES", sales)));
    }

    private static BudgetEntryRequest entry(String account, String dept, int month, String amount) {
        return new BudgetEntryRequest(account, dept, month, new BigDecimal(amount));
    }

    @Test
    void updatesExistingCellsAndCreatesMissingOnes() {
        BudgetLine march = new BudgetLine(marketing, sales, FiscalPeriod.of(2026, 3), new BigDecimal("8400.00"));
        when(budgetLineRepository.findByPeriodYear(2026)).thenReturn(List.of(march));

        SaveBudgetResponse response = service.save(new SaveBudgetRequest(2026, List.of(
                entry("6100", "SALES", 3, "9000"),
                entry("6100", "SALES", 4, "6600.50"))));

        assertThat(march.getAmount()).isEqualTo(new BigDecimal("9000.00"));
        verify(budgetLineRepository).saveAll(savedLines.capture());
        assertThat(savedLines.getValue()).singleElement().satisfies(line -> {
            assertThat(line.getPeriod()).isEqualTo(FiscalPeriod.of(2026, 4));
            assertThat(line.getAmount()).isEqualTo(new BigDecimal("6600.50"));
        });
        assertThat(response).isEqualTo(new SaveBudgetResponse(2026, 1, 1));
    }

    @Test
    void rejectsUnknownCodesWithTheExactFieldAndSavesNothing() {
        assertThatThrownBy(() -> service.save(new SaveBudgetRequest(2026, List.of(
                entry("6100", "SALES", 1, "1.00"),
                entry("9999", "NOPE", 2, "1.00")))))
                .isInstanceOfSatisfying(RequestValidationException.class, e -> assertThat(e.getErrors()).containsExactly(
                        new FieldErrorDetail("entries[1].accountCode", "Unknown account code '9999'"),
                        new FieldErrorDetail("entries[1].departmentCode", "Unknown department code 'NOPE'")));

        verify(budgetLineRepository, never()).saveAll(any());
    }

    @Test
    void rejectsTheSameCellTwiceInOneRequest() {
        assertThatThrownBy(() -> service.save(new SaveBudgetRequest(2026, List.of(
                entry("6100", "SALES", 5, "1.00"),
                entry("6100", "SALES", 5, "2.00")))))
                .isInstanceOfSatisfying(RequestValidationException.class, e -> assertThat(e.getErrors())
                        .singleElement()
                        .extracting(FieldErrorDetail::field)
                        .isEqualTo("entries[1].month"));

        verify(budgetLineRepository, never()).saveAll(any());
    }
}
