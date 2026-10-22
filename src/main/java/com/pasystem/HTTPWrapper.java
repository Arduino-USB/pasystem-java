package com.pasystem;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HTTPWrapper {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)) // Fail fast if client is dead/unreachable
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public <T> T get(String url, Class<T> type) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
				.timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP error: " + response.statusCode());
        }

        String body = response.body();
        // Handle empty or null bodies gracefully if the endpoint just returns a 200 OK status
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        return mapper.readValue(body, type);
    }
}
