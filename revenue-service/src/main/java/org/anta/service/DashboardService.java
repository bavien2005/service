// org/anta/services/revenue_service/service/DashboardService.java
package org.anta.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.anta.client.CartRevenueClient;
import org.anta.client.OrderRevenueClient;
import org.anta.dto.WeeklyRevenueComparisonDTO;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardService {

    @Inject
    @RestClient
    CartRevenueClient cartRevenueClient;

    @Inject
    @RestClient
    OrderRevenueClient orderRevenueClient;

    // 🔥 Chỉ dùng method này để FE vẽ biểu đồ
    public List<WeeklyRevenueComparisonDTO> getWeeklyRevenueComparison() {

        // ----- Gọi cart-service: /cart/revenue/weekly -----
        List<Map<String, Object>> expectedList = cartRevenueClient.getExpectedRevenueWeekly();
        if (expectedList == null) {
            expectedList = Collections.emptyList();
        }

        // ----- Gọi order-service: /orders/revenue/weekly -----
        List<Map<String, Object>> actualList = orderRevenueClient.getActualRevenueWeekly();
        if (actualList == null) {
            actualList = Collections.emptyList();
        }

        // ----- Merge theo key "week" -----
        Map<String, WeeklyRevenueComparisonDTO> map = new HashMap<>();

        for (Map<String, Object> row : expectedList) {
            String week = Objects.toString(row.get("week"), "");
            Double revenue = row.get("revenue") != null
                    ? ((Number) row.get("revenue")).doubleValue()
                    : 0.0;

            map.computeIfAbsent(
                    week,
                    w -> new WeeklyRevenueComparisonDTO(w, 0.0, 0.0)
            ).setExpectedRevenue(revenue);
        }

        for (Map<String, Object> row : actualList) {
            String week = Objects.toString(row.get("week"), "");
            Double revenue = row.get("revenue") != null
                    ? ((Number) row.get("revenue")).doubleValue()
                    : 0.0;

            map.computeIfAbsent(
                    week,
                    w -> new WeeklyRevenueComparisonDTO(w, 0.0, 0.0)
            ).setActualRevenue(revenue);
        }

        // Sắp xếp theo week format "YYYY-Www"
        return map.values().stream()
                .sorted(Comparator.comparing(WeeklyRevenueComparisonDTO::getWeek))
                .collect(Collectors.toList());
    }
}