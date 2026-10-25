package com.driverline.planning.budget;

import com.driverline.planning.budget.dto.BudgetGridResponse;
import com.driverline.planning.budget.dto.SaveBudgetRequest;
import com.driverline.planning.budget.dto.SaveBudgetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Monthly budget entry")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Get the budget grid for a year: one row per account and department, 12 monthly amounts")
    public BudgetGridResponse getGrid(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) String department) {
        return budgetService.getGrid(year, department);
    }

    @PutMapping
    @Operation(summary = "Create or update budget amounts. All-or-nothing: if any entry is invalid, nothing is saved")
    public SaveBudgetResponse save(@Valid @RequestBody SaveBudgetRequest request) {
        return budgetService.save(request);
    }
}
