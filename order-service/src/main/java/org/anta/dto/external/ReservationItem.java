package org.anta.dto.external;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationItem {

    private Long variantId;  // product variant id

    private Integer quantity;
}
