package org.anta.dto.response;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class CartItemsResponse {
    private Long id;

    private Long cartId;

    private Long productId;

    private Long variantId;

    private String productName;

    private String imageUrl;

    private Double unitPrice;

    private Long quantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Double totalAmount;

    private String size;

    private String color;

    private String sku;

}
