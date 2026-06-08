package com.pasystem;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class HTTPWrapper {
    
    private HttpClient client = HttpClient.newHttpClient();
    

    public Map get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return Map.of("response", (String) response.body(),  "status", (Integer) response.statusCode());
    }
}