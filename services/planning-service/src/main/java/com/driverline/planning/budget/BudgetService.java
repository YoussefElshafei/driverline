package com.driverline.planning.budget;

import com.driverline.planning.budget.dto.BudgetEntryRequest;
import com.driverline.planning.budget.dto.BudgetGridResponse;
import com.driverline.planning.budget.dto.SaveBudgetRequest;
import com.driverline.planning.budget.dto.SaveBudgetResponse;
import com.driverline.planning.common.LineKey;
import com.driverline.planning.common.error.FieldErrorDetail;
import com.driverline.planning.common.error.RequestValidationException;
import com.driverline.planning.period.FiscalPeriod;
import com.driverline.planning.reference.ReferenceData;
import com.driverline.planning.reference.ReferenceDataService;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetLineRepository budgetLineRepository;
    private final ReferenceDataService referenceDataService;
    private final BudgetGridAssembler gridAssembler;

    public BudgetService(
            BudgetLineRepository budgetLineRepository,
            ReferenceDataService referenceDataService,
            BudgetGridAssembler gridAssembler) {
        this.budgetLineRepository = budgetLineRepository;
        this.referenceDataService = referenceDataService;
        this.gridAssembler = gridAssembler;
    }

    /** @param departmentCode optional filter; {@code null} returns every department */
    @Transactional(readOnly = true)
    public BudgetGridResponse getGrid(int year, String departmentCode) {
        List<BudgetLine> lines = budgetLineRepository.findByPeriodYear(year);
        if (departmentCode != null) {
            requireKnownDepartment(departmentCode);
            lines = lines.stream().filter(line -> line.getDepartment().getCode().equals(departmentCode)).toList();
        }
        return new BudgetGridResponse(year, departmentCode, gridAssembler.assemble(lines));
    }

    /**
     * Creates or updates every entry in one transaction. If any entry is invalid, nothing is saved.
     */
    @Transactional
    public SaveBudgetResponse save(SaveBudgetRequest request) {
        ReferenceData referenceData = referenceDataService.load();
        validateEntries(request.entries(), referenceData);

        Map<LineKey, BudgetLine> existingLines = budgetLineRepository.findByPeriodYear(request.year()).stream()
                .collect(Collectors.toMap(BudgetService::keyOf, Function.identity()));

        List<BudgetLine> newLines = new ArrayList<>();
        int updated = 0;
        for (BudgetEntryRequest entry : request.entries()) {
            var amount = entry.amount().setScale(2, RoundingMode.UNNECESSARY);
            LineKey key = keyOf(entry, request.year());
            BudgetLine existing = existingLines.get(key);
            if (existing != null) {
                // A managed entity: Hibernate writes the change when the transaction commits (dirty checking).
                existing.changeAmount(amount);
                updated++;
            } else {
                newLines.add(new BudgetLine(
                        referenceData.account(entry.accountCode()),
                        referenceData.department(entry.departmentCode()),
                        key.period(),
                        amount));
            }
        }
        budgetLineRepository.saveAll(newLines);
        return new SaveBudgetResponse(request.year(), newLines.size(), updated);
    }

    private void requireKnownDepartment(String departmentCode) {
        if (!referenceDataService.load().departmentCodes().contains(departmentCode)) {
            throw RequestValidationException.forField(
                    "department", "Unknown department code '%s'".formatted(departmentCode));
        }
    }

    private static void validateEntries(List<BudgetEntryRequest> entries, ReferenceData referenceData) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        Map<String, Integer> firstIndexByCell = new HashMap<>();

        for (int i = 0; i < entries.size(); i++) {
            BudgetEntryRequest entry = entries.get(i);
            String prefix = "entries[%d].".formatted(i);
            if (!referenceData.accountCodes().contains(entry.accountCode())) {
                errors.add(new FieldErrorDetail(
                        prefix + "accountCode", "Unknown account code '%s'".formatted(entry.accountCode())));
            }
            if (!referenceData.departmentCodes().contains(entry.departmentCode())) {
                errors.add(new FieldErrorDetail(
                        prefix + "departmentCode", "Unknown department code '%s'".formatted(entry.departmentCode())));
            }
            String cell = entry.accountCode() + "|" + entry.departmentCode() + "|" + entry.month();
            Integer firstIndex = firstIndexByCell.putIfAbsent(cell, i);
            if (firstIndex != null) {
                errors.add(new FieldErrorDetail(
                        prefix + "month", "Duplicate of entries[%d]: same account, department and month".formatted(firstIndex)));
            }
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException("Some budget entries are invalid. Nothing was saved.", errors);
        }
    }

    private static LineKey keyOf(BudgetLine line) {
        return new LineKey(line.getAccount().getCode(), line.getDepartment().getCode(), line.getPeriod());
    }

    private static LineKey keyOf(BudgetEntryRequest entry, int year) {
        return new LineKey(entry.accountCode(), entry.departmentCode(), FiscalPeriod.of(year, entry.month()));
    }
}
