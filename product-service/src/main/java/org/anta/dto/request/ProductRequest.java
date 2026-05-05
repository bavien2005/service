package org.anta.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {


    private String name;

    private String brand;

    private String description;

    private BigDecimal price;

    private Long categoryId;

    private List<String> categories;

    private Integer totalStock;

    private List<String> images;

    private List<Long> imageIds;

    private List<ProductVariantRequest> variants;

}
