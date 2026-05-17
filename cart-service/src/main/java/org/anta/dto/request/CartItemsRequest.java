package org.anta.dto.request;

import lombok.Data;
import org.anta.entity.Carts;

@Data
public class CartItemsRequest {
    private Long userId;     // optional
    private String sessionId;// optional
    private Long productId;
    private Long variantId;  // nullable
    private Long quantity;

    // optional fields from FE (variant attributes)
    private String size;
    private String color;
    private String sku;
    private String imageUrl; // FE
}
