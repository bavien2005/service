package org.anta.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {

    private Long id;

    private String name;

    private String brand;

    private String description;

    private BigDecimal price;

    private Long categoryId;

    private List<String> images;

    private LocalDateTime createdAt;

    private String thumbnail;

    private Integer totalStock;

    private Integer rating = 5;

    private Long sales = 0L;

    private List<ProductVariantResponse> variants;

    private String categoryName;

    private String categorySlug;

    private String categoryTitle;

}
