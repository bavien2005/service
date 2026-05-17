package org.anta.dto.external;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantDto {

    private Long id;
    private Long productId;        // id của product cha
    private String sku;
    private String name;          // tên variant hoặc product name
    private String productName;   // tên product (nếu cần tách)
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;      // full image url
    private String thumbnail;     // smaller thumb
    private String size;
    private String color;
}