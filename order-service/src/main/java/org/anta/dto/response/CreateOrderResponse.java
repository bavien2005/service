package org.anta.dto.response;


import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderResponse {

    private Long orderId;
    private String status;
    private String payUrl;
    private String recipientName;
    private String recipientPhone;
    private String buyerName;
    private String email;
    private String orderNumber;
    private Long total;
    @JsonAlias({"shippingFee","shipping_fee","shippingCost","shipping_cost","shipping"})
    private Long shippingFee;
}
