package com.aurora.agents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Shared OpenAI-compatible provider client with bounded network waits.
 * Credentials are supplied per request and are never stored in the client.
 */
@Component
public class AiProviderClient {
    static final String COMPLETIONS_PATH = "/chat/completions";
    private final RestClient client;

    public AiProviderClient(@Value("${aurora.ai.base-url:https://openrouter.ai/api/v1}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        this.client = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    public Map<?, ?> complete(String apiKey, Map<String, Object> payload) {
        return client.post()
            .uri(COMPLETIONS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + apiKey)
            .body(payload)
            .retrieve()
            .body(Map.class);
    }
}
