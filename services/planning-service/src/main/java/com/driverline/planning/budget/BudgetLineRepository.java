package com.driverline.planning.budget;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetLineRepository extends JpaRepository<BudgetLine, Long> {

    /**
     * Loads every budget line for a fiscal year with its account and department in a single query.
     * Without the entity graph, reading each line's lazy account would fire one extra query per line (N+1).
     */
    @EntityGraph(attributePaths = {"account", "department"})
    List<BudgetLine> findByPeriodYear(int year);
}
