package com.driverline.planning.variance;

import com.driverline.planning.account.Account;
import com.driverline.planning.common.error.FieldErrorDetail;
import com.driverline.planning.common.error.RequestValidationException;
import com.driverline.planning.department.Department;
import com.driverline.planning.reference.ReferenceData;
import com.driverline.planning.reference.ReferenceDataService;
import com.driverline.planning.variance.dto.AccountVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.MonthlyVarianceReport;
import com.driverline.planning.variance.report.PlanningFact;
import com.driverline.planning.variance.report.PlanningFactLoader;
import com.driverline.planning.variance.report.ReportPeriod;
import com.driverline.planning.variance.report.VarianceReportBuilder;
import com.driverline.planning.variance.report.VarianceReportBuilder.MonthlyTrend;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VarianceService {

    private final PlanningFactLoader factLoader;
    private final VarianceReportBuilder reportBuilder;
    private final ReferenceDataService referenceDataService;

    public VarianceService(
            PlanningFactLoader factLoader,
            VarianceReportBuilder reportBuilder,
            ReferenceDataService referenceDataService) {
        this.factLoader = factLoader;
        this.reportBuilder = reportBuilder;
        this.referenceDataService = referenceDataService;
    }

    public DepartmentVarianceReport byDepartment(int year, Integer fromMonth, Integer toMonth) {
        List<PlanningFact> facts = factLoader.loadYear(year);
        return reportBuilder.byDepartment(facts, resolvePeriod(year, fromMonth, toMonth, facts));
    }

    public AccountVarianceReport byAccount(int year, Integer fromMonth, Integer toMonth) {
        List<PlanningFact> facts = factLoader.loadYear(year);
        return reportBuilder.byAccount(facts, resolvePeriod(year, fromMonth, toMonth, facts));
    }

    public MonthlyVarianceReport byMonth(int year, String departmentCode, String accountCode) {
        ReferenceData referenceData = referenceDataService.load();
        validateCodes(departmentCode, accountCode, referenceData);
        Department department = departmentCode == null ? null : referenceData.department(departmentCode);
        Account account = accountCode == null ? null : referenceData.account(accountCode);

        VarianceDirection direction = account == null
                ? VarianceDirection.HIGHER_IS_BETTER
                : VarianceDirection.forAccountType(account.getType());
        MonthlyTrend trend = reportBuilder.byMonth(factLoader.loadYear(year), departmentCode, accountCode, direction);

        return new MonthlyVarianceReport(
                year, departmentCode, accountCode, scopeLabel(department, account), direction,
                trend.latestActualMonth(), trend.months(), trend.closedMonthsTotal());
    }

    private ReportPeriod resolvePeriod(int year, Integer fromMonth, Integer toMonth, List<PlanningFact> facts) {
        return ReportPeriod.resolve(year, fromMonth, toMonth, reportBuilder.latestActualMonth(facts));
    }

    private static void validateCodes(String departmentCode, String accountCode, ReferenceData referenceData) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        if (departmentCode != null && !referenceData.departmentCodes().contains(departmentCode)) {
            errors.add(new FieldErrorDetail("department", "Unknown department code '%s'".formatted(departmentCode)));
        }
        if (accountCode != null && !referenceData.accountCodes().contains(accountCode)) {
            errors.add(new FieldErrorDetail("account", "Unknown account code '%s'".formatted(accountCode)));
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException("The report filters are invalid.", errors);
        }
    }

    private static String scopeLabel(Department department, Account account) {
        String where = department == null ? "all departments" : department.getName();
        if (account == null) {
            return department == null ? "Net income – all departments" : "Net contribution – " + where;
        }
        return "%s %s – %s".formatted(account.getCode(), account.getName(), where);
    }
}
