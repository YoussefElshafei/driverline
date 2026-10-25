package com.driverline.planning.commentary;

import com.driverline.planning.commentary.dto.CommentaryRequest.Figures;
import com.driverline.planning.commentary.dto.CommentaryRequest.Highlight;
import com.driverline.planning.variance.dto.AccountVarianceRow;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceRow;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Picks the variances worth explaining: the account rows (per department) with the largest dollar variance,
 * favorable or not. Dollars rather than percent, because a 50% miss on a $200 line matters less than a 2% miss
 * on a $2M line.
 */
@Component
public class VarianceHighlightSelector {

    public static final int DEFAULT_LIMIT = 5;

    private record Candidate(DepartmentVarianceRow department, AccountVarianceRow account) {
    }

    public List<Highlight> select(DepartmentVarianceReport report, int limit) {
        return report.departments().stream()
                .flatMap(department -> department.accounts().stream().map(account -> new Candidate(department, account)))
                .filter(candidate -> candidate.account().line().variance().signum() != 0)
                .sorted(Comparator
                        .comparing((Candidate c) -> c.account().line().variance().abs()).reversed()
                        // Ties are ordered by code so the selection is always the same.
                        .thenComparing(c -> c.department().departmentCode())
                        .thenComparing(c -> c.account().accountCode()))
                .limit(limit)
                .map(c -> new Highlight(c.account().accountName(), c.department().departmentName(),
                        c.account().accountType(), Figures.from(c.account().line())))
                .toList();
    }
}
