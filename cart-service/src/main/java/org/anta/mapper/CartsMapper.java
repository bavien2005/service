package org.anta.mapper;

import org.anta.dto.response.CartsResponse;
import org.anta.entity.Carts;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi", uses = {CartItemsMapper.class})
public interface CartsMapper {

    // Map từng trường rõ ràng
    @Mapping(source = "items", target = "items")
    CartsResponse toResponse(Carts carts);
}