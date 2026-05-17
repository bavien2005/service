package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.anta.dto.response.WeeklyRevenueDTO;
import org.anta.repository.CartItemsRepository;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CartRevenueService {

    @Inject
    CartItemsRepository cartItemsRepository;

    // DOANH THU DỰ KIẾN THEO TUẦN
    public List<WeeklyRevenueDTO> getExpectedRevenueWeekly() {
        List<Object[]> rows = cartItemsRepository.sumRevenueFromOpenCartsByWeek();
        List<WeeklyRevenueDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            String weekLabel = (String) row[0]; // "2025-W01"
            Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            result.add(new WeeklyRevenueDTO(weekLabel, total));
        }

        return result;
    }
}