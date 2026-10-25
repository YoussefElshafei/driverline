package com.driverline.planning.commentary.dto;

/** Commentary for a variance report period, as returned to the frontend. */
public record VarianceCommentary(
        int year,
        int fromMonth,
        int toMonth,
        String periodLabel,
        String text,
        String provider,
        String model,
        String fallbackReason) {
}
