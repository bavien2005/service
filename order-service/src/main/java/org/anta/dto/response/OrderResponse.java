package org.anta.dto.response;


import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long userId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private String payUrl;
    private String buyerName;
    private String recipientName;
    private String shippingAddress;
    private String shippingService;
    private String trackingNumber;
    private String estimatedDelivery; // ISO date string yyyy-MM-dd
    private String createdAt; // ISO string
    private List<Item> items;
    private String recipientPhone;
    private String buyerEmail;
    private Boolean refundRequested;
    @JsonAlias({"shippingFee","shipping_fee","shippingCost","shipping_cost","shipping"})
    private Long shippingFee;
    private Long discountAmount;
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long productId;
        private Long variantId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        // optional product attributes
        private String name;
        private String image;
        private String size;
        private String color;
    }
}

