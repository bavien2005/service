package org.anta.dto.request;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private Long userId;

    private List<OrderItemRequest> items;

    private String shippingAddress;

    private String paymentMethod;

    private String recipientName;

    private String recipientPhone;

    private String buyerName;

    private String email;

    private String orderNumber;

    private Long total;                // "MOMO" ...

    @JsonAlias({"shippingFee","shipping_fee","shippingCost","shipping_cost","shipping"})
    private Long shippingFee;        // phí vận chuyển (vietnamdong)

    private String shippingMethod; // "standard" | "express" | "superExpress"

    private Integer discount;      // nếu percent, hoặc fixed tuỳ loại (cần thống nhất)

    private Long discountAmount;

    private String promoCode;
}
