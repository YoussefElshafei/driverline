package com.driverline.planning.forecast;

import com.driverline.planning.common.excel.ExcelFile;
import com.driverline.planning.forecast.dto.DriverGridResponse;
import com.driverline.planning.forecast.dto.ForecastReport;
import com.driverline.planning.forecast.dto.SaveDriversRequest;
import com.driverline.planning.forecast.dto.SaveDriversResponse;
import com.driverline.planning.forecast.dto.ScenarioComparison;
import com.driverline.planning.forecast.dto.ScenarioResponse;
import com.driverline.planning.forecast.export.ForecastExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
@Tag(name = "Forecast", description = "Driver-based full-year forecast with Base / Best / Worst scenarios")
public class ForecastController {

    static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ForecastService forecastService;
    private final ForecastExportService exportService;

    public ForecastController(ForecastService forecastService, ForecastExportService exportService) {
        this.forecastService = forecastService;
        this.exportService = exportService;
    }

    @GetMapping
    @Operation(summary = "Full-year outlook for a scenario: actuals for closed months, driver-based forecast for the rest")
    public ForecastReport forecast(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) String scenario) {
        return forecastService.forecast(year, scenario);
    }

    @GetMapping("/compare")
    @Operation(summary = "Base, Best and Worst full-year outlooks side by side")
    public ScenarioComparison compare(@RequestParam @Min(2000) @Max(2100) int year) {
        return forecastService.compare(year);
    }

    @GetMapping("/scenarios")
    @Operation(summary = "List the scenarios")
    public List<ScenarioResponse> scenarios() {
        return forecastService.scenarios();
    }

    @GetMapping("/drivers")
    @Operation(summary = "Every forecastable line with its method and driver values per scenario")
    public DriverGridResponse drivers(@RequestParam @Min(2000) @Max(2100) int year) {
        return forecastService.drivers(year);
    }

    @PutMapping("/drivers")
    @Operation(summary = "Create or update forecast methods and driver values. All-or-nothing")
    public SaveDriversResponse saveDrivers(@Valid @RequestBody SaveDriversRequest request) {
        return forecastService.saveDrivers(request);
    }

    @GetMapping("/export")
    @Operation(summary = "The forecast for a scenario, plus a scenario comparison sheet, as an Excel workbook")
    public ResponseEntity<byte[]> export(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) String scenario) {
        ExcelFile file = exportService.export(year, scenario);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
