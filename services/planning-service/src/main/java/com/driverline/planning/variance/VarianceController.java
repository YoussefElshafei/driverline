package com.driverline.planning.variance;

import com.driverline.planning.common.excel.ExcelFile;
import com.driverline.planning.variance.dto.AccountVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.MonthlyVarianceReport;
import com.driverline.planning.variance.export.VarianceExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/variance")
@Tag(name = "Variance", description = "Budget vs. actual reports and Excel export")
public class VarianceController {

    static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final VarianceService varianceService;
    private final VarianceExportService exportService;

    public VarianceController(VarianceService varianceService, VarianceExportService exportService) {
        this.varianceService = varianceService;
        this.exportService = exportService;
    }

    @GetMapping("/by-department")
    @Operation(summary = "Company summary and each department's net contribution, with its accounts nested for drill-down. "
            + "Months default to January through the latest month with actuals")
    public DepartmentVarianceReport byDepartment(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer fromMonth,
            @RequestParam(required = false) @Min(1) @Max(12) Integer toMonth) {
        return varianceService.byDepartment(year, fromMonth, toMonth);
    }

    @GetMapping("/by-department/export")
    @Operation(summary = "The by-department report as a formatted Excel workbook")
    public ResponseEntity<byte[]> exportByDepartment(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer fromMonth,
            @RequestParam(required = false) @Min(1) @Max(12) Integer toMonth) {
        return download(exportService.byDepartment(year, fromMonth, toMonth));
    }

    @GetMapping("/by-account")
    @Operation(summary = "Profit-and-loss layout: every revenue and expense account combined across departments")
    public AccountVarianceReport byAccount(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer fromMonth,
            @RequestParam(required = false) @Min(1) @Max(12) Integer toMonth) {
        return varianceService.byAccount(year, fromMonth, toMonth);
    }

    @GetMapping("/by-account/export")
    @Operation(summary = "The by-account report as a formatted Excel workbook")
    public ResponseEntity<byte[]> exportByAccount(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer fromMonth,
            @RequestParam(required = false) @Min(1) @Max(12) Integer toMonth) {
        return download(exportService.byAccount(year, fromMonth, toMonth));
    }

    @GetMapping("/by-month")
    @Operation(summary = "12-month trend for net income, one department's net contribution, or one account")
    public MonthlyVarianceReport byMonth(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String account) {
        return varianceService.byMonth(year, department, account);
    }

    @GetMapping("/by-month/export")
    @Operation(summary = "The monthly trend as a formatted Excel workbook")
    public ResponseEntity<byte[]> exportByMonth(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String account) {
        return download(exportService.byMonth(year, department, account));
    }

    private static ResponseEntity<byte[]> download(ExcelFile file) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
