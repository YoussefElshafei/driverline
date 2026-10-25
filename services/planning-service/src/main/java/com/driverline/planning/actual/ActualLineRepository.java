package com.driverline.planning.actual;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActualLineRepository extends JpaRepository<ActualLine, Long> {

    /** Loads every actual line for a fiscal year with its account and department in one query (avoids N+1). */
    @EntityGraph(attributePaths = {"account", "department"})
    List<ActualLine> findByPeriodYear(int year);
}
