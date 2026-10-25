package com.driverline.planning.actual.importing;

import com.driverline.planning.actual.ActualLine;
import com.driverline.planning.actual.ActualLineRepository;
import com.driverline.planning.common.LineKey;
import com.driverline.planning.period.FiscalPeriod;
import com.driverline.planning.reference.ReferenceData;
import com.driverline.planning.reference.ReferenceDataService;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports actuals from a file. All-or-nothing: every row is validated first, and if any row is invalid nothing
 * is saved. Rows for an account/department/month that already has an actual replace its amount (no duplicates).
 */
@Service
public class ActualsImportService {

    private final List<ActualsFileParser> parsers;
    private final ActualRowValidator validator;
    private final ReferenceDataService referenceDataService;
    private final ActualLineRepository actualLineRepository;

    public ActualsImportService(
            List<ActualsFileParser> parsers,
            ActualRowValidator validator,
            ReferenceDataService referenceDataService,
            ActualLineRepository actualLineRepository) {
        this.parsers = parsers;
        this.validator = validator;
        this.referenceDataService = referenceDataService;
        this.actualLineRepository = actualLineRepository;
    }

    @Transactional
    public ImportSummary importFile(String fileName, InputStream content) {
        List<RawActualRow> rawRows = parserFor(fileName).parse(content);
        if (rawRows.isEmpty()) {
            throw ImportValidationException.forFile("The file has a header row but no data rows.");
        }

        ReferenceData referenceData = referenceDataService.load();
        ActualRowValidator.Result result =
                validator.validate(rawRows, referenceData.accountCodes(), referenceData.departmentCodes());
        if (result.hasErrors()) {
            throw new ImportValidationException(result.errors());
        }
        return save(fileName, result.validRows(), referenceData);
    }

    private ActualsFileParser parserFor(String fileName) {
        return parsers.stream()
                .filter(parser -> parser.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(fileName));
    }

    private ImportSummary save(String fileName, List<ValidActualRow> rows, ReferenceData referenceData) {
        Map<LineKey, ActualLine> existingLines = existingLinesFor(rows);

        List<ActualLine> newLines = new ArrayList<>();
        int updated = 0;
        for (ValidActualRow row : rows) {
            ActualLine existing = existingLines.get(row.key());
            if (existing != null) {
                existing.changeAmount(row.amount());
                updated++;
            } else {
                newLines.add(new ActualLine(
                        referenceData.account(row.accountCode()),
                        referenceData.department(row.departmentCode()),
                        row.period(),
                        row.amount()));
            }
        }
        actualLineRepository.saveAll(newLines);
        return new ImportSummary(fileName, rows.size(), newLines.size(), updated, periodsTouched(rows));
    }

    private Map<LineKey, ActualLine> existingLinesFor(List<ValidActualRow> rows) {
        return rows.stream()
                .map(row -> row.period().year())
                .distinct()
                .flatMap(year -> actualLineRepository.findByPeriodYear(year).stream())
                .collect(Collectors.toMap(
                        line -> new LineKey(line.getAccount().getCode(), line.getDepartment().getCode(), line.getPeriod()),
                        Function.identity()));
    }

    private static List<String> periodsTouched(List<ValidActualRow> rows) {
        return rows.stream()
                .map(ValidActualRow::period)
                .distinct()
                .sorted(Comparator.comparingInt(FiscalPeriod::year).thenComparingInt(FiscalPeriod::month))
                .map(period -> "%d-%02d".formatted(period.year(), period.month()))
                .toList();
    }
}
