package com.driverline.planning.forecast.export;

import com.driverline.planning.common.excel.ExcelFile;
import com.driverline.planning.forecast.ForecastService;
import com.driverline.planning.forecast.dto.ForecastReport;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ForecastExportService {

    private final ForecastService forecastService;
    private final ForecastWorkbookWriter writer;

    public ForecastExportService(ForecastService forecastService, ForecastWorkbookWriter writer) {
        this.forecastService = forecastService;
        this.writer = writer;
    }

    public ExcelFile export(int year, String scenarioCode) {
        ForecastReport report = forecastService.forecast(year, scenarioCode);
        byte[] content = writer.write(report, forecastService.compare(year));
        return new ExcelFile(
                "forecast-FY%d-%s.xlsx".formatted(year, report.scenarioCode().toLowerCase(Locale.ROOT)), content);
    }
}
