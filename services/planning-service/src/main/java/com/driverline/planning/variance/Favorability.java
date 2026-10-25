package com.driverline.planning.variance;

public enum Favorability {
    /** The variance increases profit compared to the plan. */
    FAVORABLE,
    /** The variance decreases profit compared to the plan. */
    UNFAVORABLE,
    /** Actual exactly equals budget. */
    ON_BUDGET
}
