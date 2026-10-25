package com.driverline.planning.forecast;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.common.error.FieldErrorDetail;
import com.driverline.planning.common.error.RequestValidationException;
import com.driverline.planning.forecast.dto.SaveDriversRequest.DriverLineRequest;
import com.driverline.planning.forecast.dto.SaveDriversRequest.DriverValuesRequest;
import com.driverline.planning.reference.ReferenceData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Business rules for saving drivers that annotations can't express. Collects every problem before failing,
 * with field paths such as {@code lines[2].values.BEST.rate} so the UI can point at the exact input.
 */
@Component
public class DriverRequestValidator {

    public void validate(List<DriverLineRequest> lines, ReferenceData referenceData, Set<String> scenarioCodes) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        Map<ForecastLineKey, Integer> firstIndexByLine = new HashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            DriverLineRequest line = lines.get(i);
            String prefix = "lines[%d].".formatted(i);
            Map<String, DriverValuesRequest> values = line.values() == null ? Map.of() : line.values();

            boolean knownAccount = referenceData.accountCodes().contains(line.accountCode());
            if (!knownAccount) {
                errors.add(new FieldErrorDetail(prefix + "accountCode", "Unknown account code '%s'".formatted(line.accountCode())));
            }
            if (!referenceData.departmentCodes().contains(line.departmentCode())) {
                errors.add(new FieldErrorDetail(prefix + "departmentCode",
                        "Unknown department code '%s'".formatted(line.departmentCode())));
            }
            Integer duplicateOf = firstIndexByLine.putIfAbsent(
                    new ForecastLineKey(line.accountCode(), line.departmentCode()), i);
            if (duplicateOf != null) {
                errors.add(new FieldErrorDetail(prefix + "accountCode",
                        "Duplicate of lines[%d]: same account and department".formatted(duplicateOf)));
            }
            for (String scenarioCode : values.keySet()) {
                if (!scenarioCodes.contains(scenarioCode)) {
                    errors.add(new FieldErrorDetail(prefix + "values", "Unknown scenario '%s'".formatted(scenarioCode)));
                }
            }

            switch (line.method()) {
                case UNITS_X_RATE -> requireForEveryScenario(values, scenarioCodes, prefix, errors, true, true, false);
                case PERCENT_OF_REVENUE -> {
                    if (knownAccount && referenceData.account(line.accountCode()).getType() == AccountType.REVENUE) {
                        errors.add(new FieldErrorDetail(prefix + "method",
                                "A revenue line can't be a percentage of revenue (that would be circular)"));
                    }
                    requireForEveryScenario(values, scenarioCodes, prefix, errors, false, false, true);
                }
                case BUDGET -> {
                    // Budget lines need no driver values.
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException("Some drivers are invalid. Nothing was saved.", errors);
        }
    }

    private static void requireForEveryScenario(Map<String, DriverValuesRequest> values, Set<String> scenarioCodes,
            String prefix, List<FieldErrorDetail> errors, boolean quantity, boolean rate, boolean percent) {
        for (String scenario : scenarioCodes) {
            DriverValuesRequest value = values.get(scenario);
            String path = prefix + "values." + scenario + ".";
            if (quantity && (value == null || value.quantity() == null)) {
                errors.add(new FieldErrorDetail(path + "quantity", "is required for units × rate"));
            }
            if (rate && (value == null || value.rate() == null)) {
                errors.add(new FieldErrorDetail(path + "rate", "is required for units × rate"));
            }
            if (percent && (value == null || value.percent() == null)) {
                errors.add(new FieldErrorDetail(path + "percent", "is required for percent of revenue"));
            }
        }
    }
}
