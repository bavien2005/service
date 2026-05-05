package org.anta.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.anta.dto.response.CategoryResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@ApplicationScoped
public class CategoryClient {

    @ConfigProperty(name = "category.service.url")
    String categoryServiceUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofMillis(3000))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryResponse getCategoryById(Long categoryId) {
        try {
            String url = categoryServiceUrl + "/" + categoryId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(10000))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new RuntimeException("Category not found with id: " + categoryId);
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Cannot connect to Category-Service");
            }

            return objectMapper.readValue(response.body(), CategoryResponse.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to Category-Service", e);
        }
    }

    public boolean existsCategory(Long categoryId) {
        try {
            getCategoryById(categoryId);
            return true;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Category not found")) {
                return false;
            }
            throw e;
        }
    }

    public Map<String, List<CategoryResponse>> getGrouped() {
        try {
            String url = categoryServiceUrl + "/grouped";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(10000))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Collections.emptyMap();
            }

            Map<String, List<CategoryResponse>> result = objectMapper.readValue(
                    response.body(),
                    new TypeReference<Map<String, List<CategoryResponse>>>() {
                    }
            );

            return result == null ? Collections.emptyMap() : result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public Optional<Long> resolveCategoryId(String title, String slug) {
        if (title == null || slug == null) return Optional.empty();

        String titleKey = title.toLowerCase();
        String slugKey = slug.toLowerCase();

        Map<String, List<CategoryResponse>> grouped = getGrouped();

        for (Map.Entry<String, List<CategoryResponse>> e : grouped.entrySet()) {
            String k = e.getKey() == null ? "" : e.getKey().toLowerCase();
            if (!k.equals(titleKey)) continue;

            for (CategoryResponse c : e.getValue()) {
                String s = c.getSlug() == null ? "" : c.getSlug().toLowerCase();
                if (s.equals(slugKey)) {
                    return Optional.ofNullable(c.getId());
                }
            }
        }

        return Optional.empty();
    }
}