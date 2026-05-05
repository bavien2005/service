package org.anta.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.anta.dto.response.FileMetadataDto;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@ApplicationScoped
public class CloudClient {

    @ConfigProperty(name = "cloud.service.base-url")
    String cloudBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofMillis(3000))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void updateProduct(Long productId, List<Long> imageIds) {
        try {
            String url = cloudBaseUrl + "/update-product/" + productId;
            String json = objectMapper.writeValueAsString(imageIds);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(10000))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Cloud update-product failed: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FileMetadataDto[] getFilesByProduct(Long productId) {
        try {
            String url = cloudBaseUrl + "/product/" + productId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(10000))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Cloud product files failed: " + response.statusCode());
            }

            return objectMapper.readValue(response.body(), FileMetadataDto[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object[] getFilesByProductRaw(Long productId) {
        try {
            String url = cloudBaseUrl + "/product/" + productId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(10000))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Cloud product files raw failed: " + response.statusCode());
            }

            return objectMapper.readValue(response.body(), Object[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteFile(Long fileId) {
        try {
            String url = cloudBaseUrl + "/file/" + fileId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(10000))
                    .DELETE()
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}