package com.pasystem.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;

public class HTTPWrapper {

    private final HttpClient client = HttpClient.newHttpClient();
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

        return mapper.readValue(response.body(), type);
    }


	public <T> T post(String url, Object body, Class<T> type) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						mapper.writeValueAsString(body)))
				.build();

		HttpResponse<String> response = client.send(
				request,
				HttpResponse.BodyHandlers.ofString()
		);

		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new RuntimeException("HTTP error: " + response.statusCode());
		}

		return mapper.readValue(response.body(), type);
	}


}
