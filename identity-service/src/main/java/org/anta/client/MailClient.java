package org.anta.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class MailClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    public void sendResetCodeEmail(String to, String resetCode) {

        var url = "http://localhost:8083/api/notifications/email";

        Map<String, Object> body = Map.of(
                "to", to,
                "subject", " Mã xác thực OTP của bạn",
                "body", "Xin chào,\n\nMã OTP của bạn là: "
                        + resetCode + "\nMã này có hiệu lực trong 2 phút.\n\nTrân trọng,\nĐội ngũ AntaShop",
                "idempotencyKey", UUID.randomUUID().toString()
        );

        try {
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.println("Response body: " + response.body());
                throw new RuntimeException("Notification service error: " + response.statusCode());
            }

        } catch (java.net.ConnectException e) {
            System.err.println(" Không thể kết nối tới notification-service (8083): " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.err.println("Lỗi khác khi gửi email OTP: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}