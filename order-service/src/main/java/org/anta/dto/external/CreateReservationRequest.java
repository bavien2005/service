package org.anta.dto.external;


import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequest {

    private String requestId;

    private Long userId;

    private List<ReservationItem> items;

    private Integer ttlSeconds;   // thời gian giữ tồn, ví dụ 900s = 15 phút

}