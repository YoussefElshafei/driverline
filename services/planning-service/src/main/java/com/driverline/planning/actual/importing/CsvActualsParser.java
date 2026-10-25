package com.driverline.planning.actual.importing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class CsvActualsParser implements ActualsFileParser {

    /** Excel's "CSV UTF-8" export starts the file with this invisible byte-order mark. */
    private static final int BYTE_ORDER_MARK = '﻿';

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    @Override
    public List<RawActualRow> parse(InputStream content) {
        try (Reader reader = skipByteOrderMark(new InputStreamReader(content, StandardCharsets.UTF_8));
                CSVParser parser = CSVParser.parse(reader, FORMAT)) {
            List<String> headers = parser.getHeaderNames().stream().map(ActualsFileFormat::normalizeHeader).toList();
            if (headers.isEmpty()) {
                throw ImportValidationException.forFile("The file is empty. The first row must be the column headers.");
            }
            ActualsFileFormat.requireColumns(headers);

            List<RawActualRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (isBlank(record)) {
                    continue;
                }
                rows.add(toRawRow(record));
                ActualsFileFormat.requireWithinRowLimit(rows.size());
            }
            return rows;
        } catch (IOException | UncheckedIOException e) {
            throw ImportValidationException.forFile("The file could not be read as CSV: " + e.getMessage());
        }
    }

    private static RawActualRow toRawRow(CSVRecord record) {
        // Commons CSV numbers records from 1 after the header; add 1 so it matches the line number the user sees.
        int rowNumber = Math.toIntExact(record.getRecordNumber() + 1);
        return new RawActualRow(
                rowNumber,
                value(record, ActualsFileFormat.YEAR),
                value(record, ActualsFileFormat.MONTH),
                value(record, ActualsFileFormat.ACCOUNT_CODE),
                value(record, ActualsFileFormat.DEPARTMENT_CODE),
                value(record, ActualsFileFormat.AMOUNT));
    }

    /** Short rows (fewer columns than the header) give blanks, which the validator reports as missing values. */
    private static String value(CSVRecord record, String column) {
        return record.isSet(column) ? record.get(column).trim() : "";
    }

    private static boolean isBlank(CSVRecord record) {
        return record.stream().allMatch(String::isBlank);
    }

    private static Reader skipByteOrderMark(Reader reader) throws IOException {
        PushbackReader pushback = new PushbackReader(reader, 1);
        int first = pushback.read();
        if (first != -1 && first != BYTE_ORDER_MARK) {
            pushback.unread(first);
        }
        return pushback;
    }
}
