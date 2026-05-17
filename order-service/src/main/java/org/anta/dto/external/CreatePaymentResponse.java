package org.anta.dto.external;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentResponse {

    private String payUrl;

    private String orderId;     // partner order id phía MoMo

    private String requestId;

    private Integer resultCode;

    private String message;

}