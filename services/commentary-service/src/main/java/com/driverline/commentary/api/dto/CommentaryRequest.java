package com.driverline.commentary.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * What to write about: the period, company totals, and the largest account variances (already chosen and
 * ordered by planning-service).
 *
 * @param periodLabel e.g. "FY2026, January to September"
 */
public record CommentaryRequest(
        @NotBlank @Size(max = 100) String periodLabel,
        @NotNull @Valid VarianceFigures revenue,
        @NotNull @Valid VarianceFigures expenses,
        @NotNull @Valid VarianceFigures netIncome,
        @NotNull @Size(max = 10) List<@Valid @NotNull VarianceHighlight> highlights) {
}
