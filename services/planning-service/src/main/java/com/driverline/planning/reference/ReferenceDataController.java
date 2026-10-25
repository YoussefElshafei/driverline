package com.driverline.planning.reference;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Reference data", description = "Chart of accounts and departments")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping("/accounts")
    @Operation(summary = "List the chart of accounts, ordered by code")
    public List<AccountResponse> listAccounts() {
        return referenceDataService.listAccounts();
    }

    @GetMapping("/departments")
    @Operation(summary = "List departments, ordered by code")
    public List<DepartmentResponse> listDepartments() {
        return referenceDataService.listDepartments();
    }
}
