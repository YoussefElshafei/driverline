package com.driverline.planning.variance.export;

import com.driverline.planning.variance.VarianceService;
import org.springframework.stereotype.Service;

/** Builds a report with {@link VarianceService}, lays it out, and renders it as an .xlsx file. */
@Service
public class VarianceExportService {

    public record ExcelFile(String fileName, byte[] content) {
    }

    private final VarianceService varianceService;
    private final VarianceExcelMapper mapper;
    private final VarianceWorkbookWriter writer;

    public VarianceExportService(VarianceService varianceService, VarianceExcelMapper mapper, VarianceWorkbookWriter writer) {
        this.varianceService = varianceService;
        this.mapper = mapper;
        this.writer = writer;
    }

    public ExcelFile byDepartment(int year, Integer fromMonth, Integer toMonth) {
        return render(mapper.byDepartment(varianceService.byDepartment(year, fromMonth, toMonth)));
    }

    public ExcelFile byAccount(int year, Integer fromMonth, Integer toMonth) {
        return render(mapper.byAccount(varianceService.byAccount(year, fromMonth, toMonth)));
    }

    public ExcelFile byMonth(int year, String departmentCode, String accountCode) {
        return render(mapper.byMonth(varianceService.byMonth(year, departmentCode, accountCode)));
    }

    private ExcelFile render(ExcelReport report) {
        return new ExcelFile(report.fileName(), writer.write(report));
    }
}
