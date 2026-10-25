package com.driverline.commentary.generation;

import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.VarianceFigures;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Checks language model output before anyone sees it. Language models can state numbers confidently that
 * appear nowhere in the data ("hallucinations"); in a finance report that is worse than no commentary at all.
 *
 * <p>Checks: strip reasoning tags, reject blank or overly long text, and require every number of 1,000 or more
 * to match an amount in the request (to the dollar or the cent) or a year in the period label. Smaller numbers
 * (percentages, counts) are not checked; that limitation is accepted to avoid rejecting good text.
 */
@Component
public class OutputGuard {

    public static final int MAX_LENGTH = 2000;

    /** Some models (e.g. qwen3) wrap their private reasoning in think tags before the answer. */
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");
    /** Numbers of 1,000 or more: either with thousands separators (44,059.04) or 4+ plain digits (44059). */
    private static final Pattern LARGE_NUMBER = Pattern.compile("\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?|\\d{4,}(?:\\.\\d+)?");
    /** Four digits not touching other digits, so "FY2026" yields 2026. */
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)\\d{4}(?!\\d)");

    public sealed interface Result permits Accepted, Rejected {
    }

    public record Accepted(String text) implements Result {
    }

    public record Rejected(String reason) implements Result {
    }

    public Result check(String rawOutput, CommentaryRequest request) {
        String text = THINK_BLOCK.matcher(rawOutput == null ? "" : rawOutput).replaceAll("").trim();
        if (text.isEmpty()) {
            return new Rejected("the language model returned an empty response");
        }
        if (text.length() > MAX_LENGTH) {
            return new Rejected("the language model's response was too long");
        }
        Set<String> unknownNumbers = unknownNumbers(text, request);
        if (!unknownNumbers.isEmpty()) {
            return new Rejected("the language model's response contained figures that are not in the data: "
                    + String.join(", ", unknownNumbers));
        }
        return new Accepted(text);
    }

    private static Set<String> unknownNumbers(String text, CommentaryRequest request) {
        Set<BigDecimal> allowed = allowedNumbers(request);
        Set<String> unknown = new LinkedHashSet<>();
        Matcher matcher = LARGE_NUMBER.matcher(text);
        while (matcher.find()) {
            if (!allowed.contains(normalize(matcher.group()))) {
                unknown.add(matcher.group());
            }
        }
        return unknown;
    }

    private static Set<BigDecimal> allowedNumbers(CommentaryRequest request) {
        Set<BigDecimal> allowed = new HashSet<>();
        Stream<VarianceFigures> figures = Stream.concat(
                Stream.of(request.revenue(), request.expenses(), request.netIncome()),
                request.highlights().stream().map(highlight -> highlight.figures()));
        figures.flatMap(f -> Stream.of(f.budget(), f.actual(), f.variance()))
                .forEach(amount -> {
                    allowed.add(amount.abs().stripTrailingZeros());
                    allowed.add(Amounts.roundedToDollars(amount).stripTrailingZeros());
                });
        Matcher year = YEAR.matcher(request.periodLabel());
        while (year.find()) {
            allowed.add(new BigDecimal(year.group()).stripTrailingZeros());
        }
        return allowed;
    }

    private static BigDecimal normalize(String number) {
        return new BigDecimal(number.replace(",", "")).stripTrailingZeros();
    }
}
