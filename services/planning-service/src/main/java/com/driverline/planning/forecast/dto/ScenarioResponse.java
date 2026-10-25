package com.driverline.planning.forecast.dto;

import com.driverline.planning.forecast.Scenario;

public record ScenarioResponse(String code, String name, String description) {

    public static ScenarioResponse from(Scenario scenario) {
        return new ScenarioResponse(scenario.getCode(), scenario.getName(), scenario.getDescription());
    }
}
