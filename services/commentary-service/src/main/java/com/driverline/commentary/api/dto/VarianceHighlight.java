package com.driverline.commentary.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One of the largest account variances, e.g. Contract Cleaning Revenue in Sales. */
public record VarianceHighlight(
        @NotBlank @Size(max = 100) String accountName,
        @NotBlank @Size(max = 100) String departmentName,
        @NotNull AccountType accountType,
        @NotNull @Valid VarianceFigures figures) {
}
