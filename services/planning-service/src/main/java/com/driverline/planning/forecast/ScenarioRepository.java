package com.driverline.planning.forecast;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findAllByOrderBySortOrderAsc();

    Optional<Scenario> findByCode(String code);
}
