package com.driverline.commentary.llm;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Chooses the language model from {@code commentary.llm.provider}. With "none" no LanguageModel bean exists
 * and the service always uses the rule-based writer. A new provider is one more conditional bean here.
 */
@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class LanguageModelConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    @ConditionalOnProperty(name = "commentary.llm.provider", havingValue = "ollama", matchIfMissing = true)
    LanguageModel ollamaLanguageModel(RestClient.Builder restClientBuilder, OllamaProperties properties) {
        // Fail fast when Ollama isn't running (connect), but give generation time to finish (read).
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        RestClient restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        return new OllamaLanguageModel(restClient, properties);
    }
}
