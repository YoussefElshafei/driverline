package com.driverline.planning.budget.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveBudgetRequest(
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Size(min = 1, max = 5000) List<@Valid @NotNull BudgetEntryRequest> entries) {
}
