package com.driverline.planning.seed;

import com.driverline.planning.period.FiscalPeriod;
import com.driverline.planning.seed.DemoCompanyProfile.LinePlan;
import com.driverline.planning.seed.DemoCompanyProfile.PlanYear;
import com.driverline.planning.seed.DemoDataset.SeedAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Turns a {@link DemoCompanyProfile} into monthly budget and actual amounts.
 *
 * <pre>
 * budget = monthlyBase x seasonality(month) x growth(year)
 * actual = budget x (1 + random noise) x story event factors
 * </pre>
 *
 * The random generator uses the profile's fixed seed, so the same profile always produces the same data.
 * All arithmetic is BigDecimal. Even the noise is drawn as whole basis points, never as a floating-point number.
 */
@Component
public class SeedDataGenerator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal BASIS_POINTS_PER_UNIT = BigDecimal.valueOf(10_000);

    public DemoDataset generate(DemoCompanyProfile profile) {
        Random random = new Random(profile.randomSeed());
        List<SeedAmount> budgets = new ArrayList<>();
        List<SeedAmount> actuals = new ArrayList<>();

        for (PlanYear year : profile.years()) {
            for (LinePlan plan : profile.linePlans()) {
                for (int month = 1; month <= 12; month++) {
                    FiscalPeriod period = FiscalPeriod.of(year.year(), month);
                    BigDecimal budget = budgetAmount(plan, year, month);
                    budgets.add(new SeedAmount(plan.accountCode(), plan.departmentCode(), period, budget));

                    if (month <= year.lastActualMonth()) {
                        BigDecimal actual = actualAmount(profile, plan, period, budget, random);
                        actuals.add(new SeedAmount(plan.accountCode(), plan.departmentCode(), period, actual));
                    }
                }
            }
        }
        return new DemoDataset(profile.departments(), profile.accounts(), budgets, actuals);
    }

    private BigDecimal budgetAmount(LinePlan plan, PlanYear year, int month) {
        return plan.monthlyBase()
                .multiply(plan.seasonality().factorFor(month))
                .multiply(year.growthFactor())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal actualAmount(
            DemoCompanyProfile profile, LinePlan plan, FiscalPeriod period, BigDecimal budget, Random random) {
        BigDecimal amount = budget.multiply(noiseFactor(plan.noiseBasisPoints(), random));
        for (var event : profile.storyEvents()) {
            if (event.applies(plan.accountCode(), period.year(), period.month())) {
                amount = amount.multiply(event.factor());
            }
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Returns a factor between (1 - band) and (1 + band), e.g. 0.9700 to 1.0300 for 300 basis points. */
    private BigDecimal noiseFactor(int bandBasisPoints, Random random) {
        int drawnBasisPoints = random.nextInt(-bandBasisPoints, bandBasisPoints + 1);
        return BigDecimal.ONE.add(BigDecimal.valueOf(drawnBasisPoints).divide(BASIS_POINTS_PER_UNIT));
    }
}
