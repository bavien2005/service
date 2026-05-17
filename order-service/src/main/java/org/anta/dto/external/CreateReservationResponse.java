package org.anta.dto.external;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationResponse {

    private Long reservationId;

    private String status;      // e.g., "RESERVED"

}