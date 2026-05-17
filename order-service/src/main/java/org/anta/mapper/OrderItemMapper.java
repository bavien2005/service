package org.anta.mapper;

import org.anta.dto.request.OrderItemRequest;
import org.anta.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface OrderItemMapper {
    OrderItem toOrderItem(OrderItemRequest request);
}