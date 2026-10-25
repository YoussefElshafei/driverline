package com.driverline.commentary.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/** Interactive API docs at /swagger-ui.html; the raw OpenAPI spec at /v3/api-docs. */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DriverLine commentary-service API",
        version = "v1",
        description = "Plain-English variance commentary from a swappable language model, with a rule-based fallback."))
public class OpenApiConfiguration {
}
