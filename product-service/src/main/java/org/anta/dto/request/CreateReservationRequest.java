package org.anta.dto.request;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CreateReservationRequest {

    @NotEmpty
    private List<ReservationLine> items;

    private String requestId;

    private Integer ttlSeconds;

    @Data
    public static class ReservationLine {

        private Long variantId;

        private Integer quantity;
    }
}

