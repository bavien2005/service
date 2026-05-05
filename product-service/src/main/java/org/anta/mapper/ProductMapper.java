package org.anta.mapper;

import org.anta.dto.request.ProductRequest;
import org.anta.dto.response.ProductResponse;
import org.anta.entity.Product;
import org.anta.entity.ProductVariant;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "jakarta-cdi", uses = {ProductVariantMapper.class})
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> entities);

    @Mapping(target = "id", ignore = true)
    Product toEntity(ProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "variants", ignore = true),
            @Mapping(target = "images", ignore = true)
    })
    void updateFromRequest(ProductRequest request, @MappingTarget Product product);

    @AfterMapping
    default void setParentForVariants(@MappingTarget Product product) {
        if (product == null || product.getVariants() == null) return;
        for (ProductVariant v : product.getVariants()) {
            v.setProduct(product);
        }
    }

    default Product toEntityWithParents(ProductRequest request) {
        Product p = toEntity(request);
        if (p != null && p.getVariants() != null) {
            for (ProductVariant v : p.getVariants()) {
                if (v.getProduct() == null) v.setProduct(p);
            }
        }
        return p;
    }
}