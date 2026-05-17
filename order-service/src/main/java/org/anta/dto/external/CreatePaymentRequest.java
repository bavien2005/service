package org.anta.dto.external;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    private Long orderId;

    private Long userId;

    private Long amount;   // đơn vị VND (long)

}