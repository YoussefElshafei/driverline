package com.driverline.planning.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/** Interactive API docs are served at /swagger-ui.html; the raw OpenAPI spec at /v3/api-docs. */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DriverLine planning-service API",
        version = "v1",
        description = "Accounts, departments, budgets and actuals for the DriverLine FP&A app."))
public class OpenApiConfiguration {
}
