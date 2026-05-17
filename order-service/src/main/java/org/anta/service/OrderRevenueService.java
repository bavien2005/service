package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.dto.response.ProductSoldQtyDTO;
import org.anta.dto.response.WeeklyRevenueDTO;
import org.anta.repository.OrderItemRepository;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OrderRevenueService {

    @Inject
    OrderItemRepository orderItemsRepository;

    // DOANH THU THỰC TẾ THEO TUẦN
    public List<WeeklyRevenueDTO> getActualRevenueWeekly() {
        List<Object[]> rows = orderItemsRepository.sumWeeklyRevenueFromCompletedOrders();
        List<WeeklyRevenueDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            String weekLabel = (String) row[0];
            Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            result.add(new WeeklyRevenueDTO(weekLabel, total));
        }

        return result;
    }

    @Transactional
    public List<ProductSoldQtyDTO> getSoldQtyByProductFromPaidOrDelivered() {
        List<Object[]> rows = orderItemsRepository.sumSoldQtyByProductFromPaidOrDelivered();

        return rows.stream()
                .map(r -> new ProductSoldQtyDTO(
                        r[0] == null ? null : ((Number) r[0]).longValue(),
                        r[1] == null ? 0L : ((Number) r[1]).longValue()
                ))
                .toList();
    }
}
