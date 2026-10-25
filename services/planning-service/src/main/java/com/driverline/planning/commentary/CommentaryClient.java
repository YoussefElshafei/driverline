package com.driverline.planning.commentary;

import com.driverline.planning.commentary.dto.CommentaryRequest;
import com.driverline.planning.commentary.dto.CommentaryResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP client for commentary-service. Language model problems are handled inside commentary-service (it falls
 * back to rule-based text), so an error here means the commentary service itself is down or broken.
 */
public class CommentaryClient {

    private final RestClient restClient;

    /** @param restClient a client whose base URL is commentary-service */
    public CommentaryClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public CommentaryResponse generate(CommentaryRequest request) {
        try {
            CommentaryResponse response = restClient.post()
                    .uri("/api/commentary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CommentaryResponse.class);
            if (response == null || response.text() == null) {
                throw new CommentaryUnavailableException("The commentary service returned an empty response.", null);
            }
            return response;
        } catch (ResourceAccessException e) {
            throw new CommentaryUnavailableException(
                    "The commentary service is not reachable. Start commentary-service and try again.", e);
        } catch (RestClientResponseException e) {
            throw new CommentaryUnavailableException(
                    "The commentary service returned an error (HTTP %d).".formatted(e.getStatusCode().value()), e);
        }
    }
}
