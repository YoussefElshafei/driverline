package com.driverline.planning.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Seeds the Ace Janitorial demo company and its forecast drivers on startup. Turn off with SEED_DEMO_DATA=false. */
@Component
@ConditionalOnProperty(name = "driverline.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final DemoDataLoader loader;
    private final DemoForecastLoader forecastLoader;

    public DemoDataSeeder(DemoDataLoader loader, DemoForecastLoader forecastLoader) {
        this.loader = loader;
        this.forecastLoader = forecastLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        loader.loadIfEmpty(AceJanitorialProfile.create());
        forecastLoader.loadIfMissing(AceJanitorialForecastDrivers.YEAR, AceJanitorialForecastDrivers.drivers());
    }
}
