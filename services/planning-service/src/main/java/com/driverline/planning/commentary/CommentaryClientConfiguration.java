package com.driverline.planning.commentary;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CommentaryProperties.class)
public class CommentaryClientConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /**
     * Built from Spring Boot's RestClient.Builder, so requests use the application's JSON settings
     * (money is sent as strings, like every other API response).
     */
    @Bean
    CommentaryClient commentaryClient(RestClient.Builder restClientBuilder, CommentaryProperties properties) {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(properties.timeout());
        return new CommentaryClient(restClientBuilder
                .baseUrl(properties.serviceUrl())
                .requestFactory(requestFactory)
                .build());
    }
}
