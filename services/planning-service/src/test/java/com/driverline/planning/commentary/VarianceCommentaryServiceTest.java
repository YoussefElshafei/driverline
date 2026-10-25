package com.driverline.planning.commentary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.commentary.dto.CommentaryRequest;
import com.driverline.planning.commentary.dto.CommentaryResponse;
import com.driverline.planning.commentary.dto.VarianceCommentary;
import com.driverline.planning.variance.VarianceCalculator;
import com.driverline.planning.variance.VarianceDirection;
import com.driverline.planning.variance.VarianceLine;
import com.driverline.planning.variance.VarianceService;
import com.driverline.planning.variance.dto.AccountVarianceRow;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceRow;
import com.driverline.planning.variance.dto.VarianceSummary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VarianceCommentaryServiceTest {

    @Mock
    private VarianceService varianceService;

    @Mock
    private CommentaryClient commentaryClient;

    @Captor
    private ArgumentCaptor<CommentaryRequest> sent;

    private static VarianceLine line(String budget, String actual) {
        return VarianceCalculator.calculate(new BigDecimal(budget), new BigDecimal(actual), VarianceDirection.HIGHER_IS_BETTER);
    }

    @Test
    void sendsThePeriodTotalsAndLargestVariancesAndReturnsTheCommentary() {
        VarianceLine revenue = line("1000.00", "900.00");
        VarianceLine expenses = line("800.00", "820.00");
        VarianceLine net = line("200.00", "80.00");
        when(varianceService.byDepartment(2026, null, null)).thenReturn(new DepartmentVarianceReport(2026, 1, 9, 9,
                new VarianceSummary(revenue, expenses, net), List.of(new DepartmentVarianceRow("SALES", "Sales", net,
                        List.of(new AccountVarianceRow("4000", "Contract Revenue", AccountType.REVENUE, revenue))))));
        when(commentaryClient.generate(any())).thenReturn(
                new CommentaryResponse("Revenue was $100 below budget.", "ollama", "llama3.1:8b", null));

        VarianceCommentary commentary = new VarianceCommentaryService(
                varianceService, new VarianceHighlightSelector(), commentaryClient).commentary(2026, null, null);

        verify(commentaryClient).generate(sent.capture());
        assertThat(sent.getValue().periodLabel()).isEqualTo("FY2026, January to September");
        assertThat(sent.getValue().netIncome().variance()).isEqualByComparingTo("-120.00");
        assertThat(sent.getValue().highlights()).singleElement().satisfies(highlight -> {
            assertThat(highlight.accountName()).isEqualTo("Contract Revenue");
            assertThat(highlight.departmentName()).isEqualTo("Sales");
        });
        assertThat(commentary).isEqualTo(new VarianceCommentary(2026, 1, 9, "FY2026, January to September",
                "Revenue was $100 below budget.", "ollama", "llama3.1:8b", null));
    }

    @Test
    void labelsASingleMonthPeriodWithoutARange() {
        assertThat(VarianceCommentaryService.periodLabel(2026, 3, 3)).isEqualTo("FY2026, March");
        assertThat(VarianceCommentaryService.periodLabel(2025, 1, 12)).isEqualTo("FY2025, January to December");
    }
}
