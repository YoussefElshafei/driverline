package com.driverline.planning.commentary;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from {@code commentary.*} (environment: COMMENTARY_SERVICE_URL, COMMENTARY_TIMEOUT_SECONDS). */
@ConfigurationProperties(prefix = "commentary")
public record CommentaryProperties(String serviceUrl, Duration timeout) {
}
