package org.anta.dto.request;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    private Long productId;     // optional, để hiển thị

    private Long variantId;     // bắt buộc để giữ tồn

    private Integer quantity;

    private BigDecimal unitPrice;

    private String note;        // optional

}