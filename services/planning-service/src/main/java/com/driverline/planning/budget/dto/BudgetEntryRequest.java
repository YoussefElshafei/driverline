package com.driverline.planning.budget.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** One budget cell to create or update. The amount is accepted as a JSON string or number. */
public record BudgetEntryRequest(
        @NotBlank String accountCode,
        @NotBlank String departmentCode,
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Digits(integer = 13, fraction = 2, message = "must have at most 13 digits and 2 decimal places")
        BigDecimal amount) {
}
