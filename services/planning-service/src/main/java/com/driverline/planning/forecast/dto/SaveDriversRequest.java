package com.driverline.planning.forecast.dto;

import com.driverline.planning.forecast.ForecastMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SaveDriversRequest(
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Size(min = 1, max = 500) List<@Valid @NotNull DriverLineRequest> lines) {

    /**
     * @param values driver values keyed by scenario code (BASE, BEST, WORST). Required for every scenario when the
     *               method is UNITS_X_RATE or PERCENT_OF_REVENUE; ignored for BUDGET.
     */
    public record DriverLineRequest(
            @NotBlank String accountCode,
            @NotBlank String departmentCode,
            @NotNull ForecastMethod method,
            @Size(max = 60) String quantityLabel,
            @Size(max = 60) String rateLabel,
            Map<String, @Valid @NotNull DriverValuesRequest> values) {
    }

    public record DriverValuesRequest(
            @DecimalMin("0") @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places")
            BigDecimal quantity,
            @DecimalMin("0") @Digits(integer = 13, fraction = 2, message = "must have at most 2 decimal places")
            BigDecimal rate,
            @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4, message = "must have at most 4 decimal places")
            BigDecimal percent) {
    }
}
