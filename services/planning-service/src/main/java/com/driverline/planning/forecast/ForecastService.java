package com.driverline.planning.forecast;

import com.driverline.planning.common.error.RequestValidationException;
import com.driverline.planning.forecast.dto.DriverGridResponse;
import com.driverline.planning.forecast.dto.DriverGridResponse.DriverLine;
import com.driverline.planning.forecast.dto.DriverGridResponse.DriverValues;
import com.driverline.planning.forecast.dto.ForecastReport;
import com.driverline.planning.forecast.dto.SaveDriversRequest;
import com.driverline.planning.forecast.dto.SaveDriversRequest.DriverLineRequest;
import com.driverline.planning.forecast.dto.SaveDriversRequest.DriverValuesRequest;
import com.driverline.planning.forecast.dto.SaveDriversResponse;
import com.driverline.planning.forecast.dto.ScenarioComparison;
import com.driverline.planning.forecast.dto.ScenarioComparison.ScenarioOutlook;
import com.driverline.planning.forecast.dto.ScenarioResponse;
import com.driverline.planning.forecast.engine.ForecastEngine;
import com.driverline.planning.forecast.engine.ForecastInputAssembler;
import com.driverline.planning.forecast.engine.ForecastInputLine;
import com.driverline.planning.forecast.engine.ForecastedLine;
import com.driverline.planning.reference.ReferenceData;
import com.driverline.planning.reference.ReferenceDataService;
import com.driverline.planning.variance.report.PlanningFact;
import com.driverline.planning.variance.report.PlanningFactLoader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forecasts are calculated on request from budgets, actuals and drivers, never stored,
 * so changing a driver or importing a month of actuals is reflected immediately.
 */
@Service
public class ForecastService {

    private static final String DEFAULT_QUANTITY_LABEL = "Quantity";
    private static final String DEFAULT_RATE_LABEL = "Rate";

    private final PlanningFactLoader factLoader;
    private final ForecastLineRepository forecastLineRepository;
    private final ScenarioRepository scenarioRepository;
    private final ReferenceDataService referenceDataService;
    private final ForecastInputAssembler inputAssembler;
    private final ForecastEngine engine;
    private final ForecastReportBuilder reportBuilder;
    private final DriverRequestValidator driverValidator;

    public ForecastService(
            PlanningFactLoader factLoader,
            ForecastLineRepository forecastLineRepository,
            ScenarioRepository scenarioRepository,
            ReferenceDataService referenceDataService,
            ForecastInputAssembler inputAssembler,
            ForecastEngine engine,
            ForecastReportBuilder reportBuilder,
            DriverRequestValidator driverValidator) {
        this.factLoader = factLoader;
        this.forecastLineRepository = forecastLineRepository;
        this.scenarioRepository = scenarioRepository;
        this.referenceDataService = referenceDataService;
        this.inputAssembler = inputAssembler;
        this.engine = engine;
        this.reportBuilder = reportBuilder;
        this.driverValidator = driverValidator;
    }

    @Transactional(readOnly = true)
    public List<ScenarioResponse> scenarios() {
        return scenarioRepository.findAllByOrderBySortOrderAsc().stream().map(ScenarioResponse::from).toList();
    }

    /** @param scenarioCode BASE, BEST or WORST; {@code null} means BASE */
    @Transactional(readOnly = true)
    public ForecastReport forecast(int year, String scenarioCode) {
        Scenario scenario = requireScenario(scenarioCode == null ? Scenario.BASE : scenarioCode);
        List<PlanningFact> facts = factLoader.loadYear(year);
        Set<Integer> closedMonths = PlanningFact.closedMonths(facts);
        List<ForecastedLine> lines = forecastLines(facts, forecastLineRepository.findByFiscalYear(year), scenario, closedMonths);
        return reportBuilder.build(year, scenario, latestMonth(closedMonths), lines);
    }

    @Transactional(readOnly = true)
    public ScenarioComparison compare(int year) {
        List<PlanningFact> facts = factLoader.loadYear(year);
        List<ForecastLine> configuration = forecastLineRepository.findByFiscalYear(year);
        Set<Integer> closedMonths = PlanningFact.closedMonths(facts);

        List<ScenarioOutlook> outlooks = scenarioRepository.findAllByOrderBySortOrderAsc().stream()
                .map(scenario -> new ScenarioOutlook(scenario.getCode(), scenario.getName(),
                        reportBuilder.summarize(forecastLines(facts, configuration, scenario, closedMonths))))
                .toList();
        return new ScenarioComparison(year, latestMonth(closedMonths), outlooks);
    }

    @Transactional(readOnly = true)
    public DriverGridResponse drivers(int year) {
        List<PlanningFact> facts = factLoader.loadYear(year);
        Set<Integer> closedMonths = PlanningFact.closedMonths(facts);
        Map<ForecastLineKey, ForecastLine> configured = configurationByKey(forecastLineRepository.findByFiscalYear(year));
        List<ScenarioResponse> scenarios = scenarios();

        // The Base inputs list every forecastable line (budgeted, actual or configured) in report order.
        List<ForecastInputLine> allLines = inputAssembler.assemble(facts, List.copyOf(configured.values()), Scenario.BASE);
        List<DriverLine> lines = allLines.stream()
                .map(input -> toDriverLine(input, configured.get(new ForecastLineKey(input.accountCode(), input.departmentCode())),
                        closedMonths))
                .toList();
        return new DriverGridResponse(year, latestMonth(closedMonths), scenarios, lines);
    }

    /** Creates or updates the listed lines in one transaction. If any line is invalid, nothing is saved. */
    @Transactional
    public SaveDriversResponse saveDrivers(SaveDriversRequest request) {
        ReferenceData referenceData = referenceDataService.load();
        Map<String, Scenario> scenariosByCode = scenarioRepository.findAllByOrderBySortOrderAsc().stream()
                .collect(Collectors.toMap(Scenario::getCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        driverValidator.validate(request.lines(), referenceData, scenariosByCode.keySet());

        Map<ForecastLineKey, ForecastLine> existing = configurationByKey(forecastLineRepository.findByFiscalYear(request.year()));
        for (DriverLineRequest lineRequest : request.lines()) {
            ForecastLineKey key = new ForecastLineKey(lineRequest.accountCode(), lineRequest.departmentCode());
            String quantityLabel = labelOrDefault(lineRequest.method(), lineRequest.quantityLabel(), DEFAULT_QUANTITY_LABEL);
            String rateLabel = labelOrDefault(lineRequest.method(), lineRequest.rateLabel(), DEFAULT_RATE_LABEL);

            ForecastLine line = existing.get(key);
            if (line == null) {
                line = forecastLineRepository.save(new ForecastLine(request.year(),
                        referenceData.account(key.accountCode()), referenceData.department(key.departmentCode()),
                        lineRequest.method(), quantityLabel, rateLabel));
            } else {
                line.changeMethod(lineRequest.method(), quantityLabel, rateLabel);
            }
            applyValues(line, lineRequest, scenariosByCode);
        }
        return new SaveDriversResponse(request.year(), request.lines().size());
    }

    private List<ForecastedLine> forecastLines(
            List<PlanningFact> facts, List<ForecastLine> configuration, Scenario scenario, Set<Integer> closedMonths) {
        return engine.forecast(inputAssembler.assemble(facts, configuration, scenario.getCode()), closedMonths);
    }

    /** Stores only the values the method uses, so a line never carries stale numbers from a previous method. */
    private static void applyValues(ForecastLine line, DriverLineRequest request, Map<String, Scenario> scenariosByCode) {
        if (request.method() == ForecastMethod.BUDGET) {
            return;
        }
        boolean unitsTimesRate = request.method() == ForecastMethod.UNITS_X_RATE;
        scenariosByCode.forEach((code, scenario) -> {
            DriverValuesRequest value = request.values().get(code);
            line.setValues(scenario,
                    unitsTimesRate ? value.quantity() : null,
                    unitsTimesRate ? value.rate() : null,
                    unitsTimesRate ? null : value.percent());
        });
    }

    private static DriverLine toDriverLine(ForecastInputLine input, ForecastLine configured, Set<Integer> closedMonths) {
        Map<String, DriverValues> values = new LinkedHashMap<>();
        if (configured != null) {
            configured.getValues().forEach(value -> values.put(value.getScenario().getCode(),
                    new DriverValues(value.getQuantity(), value.getRate(), value.getPercent())));
        }
        return new DriverLine(
                input.accountCode(), input.accountName(), input.accountType(),
                input.departmentCode(), input.departmentName(),
                configured == null ? ForecastMethod.BUDGET : configured.getMethod(),
                configured == null ? null : configured.getQuantityLabel(),
                configured == null ? null : configured.getRateLabel(),
                configured != null,
                values,
                averageOpenMonthBudget(input, closedMonths));
    }

    private static BigDecimal averageOpenMonthBudget(ForecastInputLine input, Set<Integer> closedMonths) {
        List<Integer> openMonths = IntStream.rangeClosed(1, 12)
                .filter(month -> !closedMonths.contains(month)).boxed().toList();
        if (openMonths.isEmpty()) {
            return null;
        }
        BigDecimal total = openMonths.stream()
                .map(month -> input.budgetByMonth().getOrDefault(month, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(openMonths.size()), 2, RoundingMode.HALF_UP);
    }

    private static Map<ForecastLineKey, ForecastLine> configurationByKey(List<ForecastLine> lines) {
        return lines.stream().collect(Collectors.toMap(
                line -> new ForecastLineKey(line.getAccount().getCode(), line.getDepartment().getCode()),
                Function.identity()));
    }

    private static String labelOrDefault(ForecastMethod method, String label, String defaultLabel) {
        if (method != ForecastMethod.UNITS_X_RATE) {
            return null;
        }
        return label == null || label.isBlank() ? defaultLabel : label.trim();
    }

    private Scenario requireScenario(String code) {
        return scenarioRepository.findByCode(code).orElseThrow(() ->
                RequestValidationException.forField("scenario", "Unknown scenario '%s'".formatted(code)));
    }

    private static int latestMonth(Set<Integer> closedMonths) {
        return closedMonths.stream().max(Integer::compare).orElse(0);
    }
}
