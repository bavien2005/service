package org.anta.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductDTO {
    private Long productId;
    private String productName;
    private Long totalQuantity;
}