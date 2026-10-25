package com.driverline.planning.commentary;

import com.driverline.planning.commentary.dto.VarianceCommentary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/variance/commentary")
@Tag(name = "Variance", description = "Budget vs. actual reports and Excel export")
public class CommentaryController {

    private final VarianceCommentaryService commentaryService;

    public CommentaryController(VarianceCommentaryService commentaryService) {
        this.commentaryService = commentaryService;
    }

    @GetMapping
    @Operation(summary = "Plain-English commentary on the largest variances for the period, written by "
            + "commentary-service (local language model, or rule-based fallback)")
    public VarianceCommentary commentary(
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer fromMonth,
            @RequestParam(required = false) @Min(1) @Max(12) Integer toMonth) {
        return commentaryService.commentary(year, fromMonth, toMonth);
    }
}
