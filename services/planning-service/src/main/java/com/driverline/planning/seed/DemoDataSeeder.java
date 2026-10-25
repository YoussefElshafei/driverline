package com.driverline.planning.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Seeds the Ace Janitorial demo company on startup. Turn off with SEED_DEMO_DATA=false. */
@Component
@ConditionalOnProperty(name = "driverline.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final DemoDataLoader loader;

    public DemoDataSeeder(DemoDataLoader loader) {
        this.loader = loader;
    }

    @Override
    public void run(ApplicationArguments args) {
        loader.loadIfEmpty(AceJanitorialProfile.create());
    }
}
