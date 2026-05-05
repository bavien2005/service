package org.anta.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductVariantRequest {

    private Long id;

    private Long productId;

    private String sku;

    private BigDecimal price;

    private Integer stock;

    private String size;

    private String color;

    private Map<String, String> attributes;

}
