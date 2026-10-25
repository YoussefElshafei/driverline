package com.driverline.planning.commentary;

import com.driverline.planning.commentary.dto.CommentaryRequest;
import com.driverline.planning.commentary.dto.CommentaryRequest.Figures;
import com.driverline.planning.commentary.dto.CommentaryResponse;
import com.driverline.planning.commentary.dto.VarianceCommentary;
import com.driverline.planning.variance.VarianceService;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Builds the variance report for a period, picks the largest variances, and asks commentary-service to explain
 * them. planning-service owns the numbers; commentary-service only turns them into words (ADR 0016).
 */
@Service
public class VarianceCommentaryService {

    private final VarianceService varianceService;
    private final VarianceHighlightSelector highlightSelector;
    private final CommentaryClient commentaryClient;

    public VarianceCommentaryService(VarianceService varianceService, VarianceHighlightSelector highlightSelector,
            CommentaryClient commentaryClient) {
        this.varianceService = varianceService;
        this.highlightSelector = highlightSelector;
        this.commentaryClient = commentaryClient;
    }

    public VarianceCommentary commentary(int year, Integer fromMonth, Integer toMonth) {
        DepartmentVarianceReport report = varianceService.byDepartment(year, fromMonth, toMonth);
        String periodLabel = periodLabel(report.year(), report.fromMonth(), report.toMonth());

        CommentaryResponse response = commentaryClient.generate(new CommentaryRequest(
                periodLabel,
                Figures.from(report.summary().revenue()),
                Figures.from(report.summary().expenses()),
                Figures.from(report.summary().netIncome()),
                highlightSelector.select(report, VarianceHighlightSelector.DEFAULT_LIMIT)));

        return new VarianceCommentary(report.year(), report.fromMonth(), report.toMonth(), periodLabel,
                response.text(), response.provider(), response.model(), response.fallbackReason());
    }

    static String periodLabel(int year, int fromMonth, int toMonth) {
        return fromMonth == toMonth
                ? "FY%d, %s".formatted(year, monthName(fromMonth))
                : "FY%d, %s to %s".formatted(year, monthName(fromMonth), monthName(toMonth));
    }

    private static String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
