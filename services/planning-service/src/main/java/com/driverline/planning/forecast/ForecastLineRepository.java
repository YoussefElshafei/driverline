package com.driverline.planning.forecast;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastLineRepository extends JpaRepository<ForecastLine, Long> {

    /** Loads a year's forecast lines with their account, department and driver values (and scenarios) in one query. */
    @EntityGraph(attributePaths = {"account", "department", "values", "values.scenario"})
    List<ForecastLine> findByFiscalYear(int fiscalYear);

    long countByFiscalYear(int fiscalYear);
}
