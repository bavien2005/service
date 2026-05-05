package org.anta.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@ApplicationScoped
public class ProductClient {

    @ConfigProperty(name = "product.service.url")
    String productServiceUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofMillis(3000))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public int deleteProductsByCategory(Long categoryId) {
        try {
            String url = productServiceUrl + "/by-category/" + categoryId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(10000))
                    .DELETE()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Cannot delete products by category. Status: " + response.statusCode());
            }

            Map<String, Object> body = objectMapper.readValue(
                    response.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            if (body == null) {
                return 0;
            }

            Object deleted = body.get("deletedProducts");

            if (deleted instanceof Number n) {
                return n.intValue();
            }

            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to Product-Service", e);
        }
    }
}