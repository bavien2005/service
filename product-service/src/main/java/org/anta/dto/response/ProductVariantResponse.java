package org.anta.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ProductVariantResponse {

    private Long id;

    private Long productId;

    private String productName;

    private String sku;

    private BigDecimal price;

    private Integer stock;

    private String size;

    private String color;

    private Map<String, String> attributes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
