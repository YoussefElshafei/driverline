package com.driverline.planning.seed;

import com.driverline.planning.forecast.ForecastLine;
import com.driverline.planning.forecast.ForecastLineRepository;
import com.driverline.planning.forecast.Scenario;
import com.driverline.planning.forecast.ScenarioRepository;
import com.driverline.planning.reference.ReferenceData;
import com.driverline.planning.reference.ReferenceDataService;
import com.driverline.planning.seed.AceJanitorialForecastDrivers.DriverSeed;
import com.driverline.planning.seed.AceJanitorialForecastDrivers.Values;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the demo forecast drivers when the demo company exists but has no forecast for the year yet.
 * Kept separate from {@link DemoDataLoader} so databases seeded before forecasting existed also receive drivers.
 */
@Service
public class DemoForecastLoader {

    private static final Logger log = LoggerFactory.getLogger(DemoForecastLoader.class);

    private final ForecastLineRepository forecastLineRepository;
    private final ScenarioRepository scenarioRepository;
    private final ReferenceDataService referenceDataService;

    public DemoForecastLoader(ForecastLineRepository forecastLineRepository, ScenarioRepository scenarioRepository,
            ReferenceDataService referenceDataService) {
        this.forecastLineRepository = forecastLineRepository;
        this.scenarioRepository = scenarioRepository;
        this.referenceDataService = referenceDataService;
    }

    /** @return true if drivers were loaded */
    @Transactional
    public boolean loadIfMissing(int year, List<DriverSeed> drivers) {
        if (forecastLineRepository.countByFiscalYear(year) > 0) {
            log.info("Demo forecast not loaded: FY{} already has forecast drivers", year);
            return false;
        }
        ReferenceData referenceData = referenceDataService.load();
        boolean demoCompanyPresent = drivers.stream().allMatch(driver ->
                referenceData.accountCodes().contains(driver.accountCode())
                        && referenceData.departmentCodes().contains(driver.departmentCode()));
        if (!demoCompanyPresent) {
            log.info("Demo forecast not loaded: the demo company's accounts and departments are not present");
            return false;
        }

        Map<String, Scenario> scenarios = scenarioRepository.findAll().stream()
                .collect(Collectors.toMap(Scenario::getCode, Function.identity()));
        List<ForecastLine> lines = drivers.stream().map(driver -> {
            ForecastLine line = new ForecastLine(year, referenceData.account(driver.accountCode()),
                    referenceData.department(driver.departmentCode()), driver.method(),
                    driver.quantityLabel(), driver.rateLabel());
            setValues(line, scenarios.get(Scenario.BASE), driver.base());
            setValues(line, scenarios.get(Scenario.BEST), driver.best());
            setValues(line, scenarios.get(Scenario.WORST), driver.worst());
            return line;
        }).toList();
        forecastLineRepository.saveAll(lines);

        log.info("Loaded demo forecast drivers for FY{}: {} driver-based lines", year, lines.size());
        return true;
    }

    private static void setValues(ForecastLine line, Scenario scenario, Values values) {
        line.setValues(scenario, values.quantity(), values.rate(), values.percent());
    }
}
