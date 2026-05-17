package org.anta.mapper;

import org.anta.dto.request.CartItemsRequest;
import org.anta.dto.response.CartItemsResponse;
import org.anta.entity.CartItems;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi")
public interface CartItemsMapper {

    @Mapping(source = "cart.id", target = "cartId")
    @Mapping(expression = "java(entity.getUnitPrice() != null && entity.getQuantity() != null ? entity.getUnitPrice() * entity.getQuantity() : 0.0)", target = "totalAmount")
    CartItemsResponse toResponse(CartItems entity);

    CartItems toEntity(CartItemsRequest dto);
}