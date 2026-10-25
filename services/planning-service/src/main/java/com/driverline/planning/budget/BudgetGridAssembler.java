package com.driverline.planning.budget;

import com.driverline.planning.budget.dto.BudgetGridRow;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Pivots flat budget lines (one per month) into grid rows (one per account and department, with 12 month columns).
 * Kept separate from the service so the pivot logic can be unit-tested without a database.
 */
@Component
public class BudgetGridAssembler {

    private static final Comparator<BudgetLine> GRID_ORDER = Comparator
            .comparing((BudgetLine line) -> line.getDepartment().getCode())
            .thenComparing(line -> line.getAccount().getCode());

    public List<BudgetGridRow> assemble(List<BudgetLine> lines) {
        Map<String, List<BudgetLine>> linesByRow = new LinkedHashMap<>();
        lines.stream()
                .sorted(GRID_ORDER)
                .forEach(line -> linesByRow
                        .computeIfAbsent(rowKey(line), key -> new ArrayList<>())
                        .add(line));

        return linesByRow.values().stream().map(BudgetGridAssembler::toRow).toList();
    }

    private static String rowKey(BudgetLine line) {
        return line.getDepartment().getCode() + "|" + line.getAccount().getCode();
    }

    private static BudgetGridRow toRow(List<BudgetLine> rowLines) {
        BigDecimal[] months = new BigDecimal[12];
        for (BudgetLine line : rowLines) {
            months[line.getPeriod().month() - 1] = line.getAmount();
        }
        BigDecimal total = Arrays.stream(months)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        BudgetLine first = rowLines.getFirst();
        return new BudgetGridRow(
                first.getAccount().getCode(),
                first.getAccount().getName(),
                first.getAccount().getType(),
                first.getDepartment().getCode(),
                first.getDepartment().getName(),
                // Arrays.asList (not List.of) because months without a budget are null.
                Arrays.asList(months),
                total);
    }
}
