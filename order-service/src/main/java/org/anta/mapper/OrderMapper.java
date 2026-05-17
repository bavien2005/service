package org.anta.mapper;

import org.anta.entity.Order;
import org.anta.entity.OrderItem;
import org.anta.dto.response.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "jakarta-cdi")
public interface OrderMapper {

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "items", expression = "java(mapItems(order.getItems()))")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "buyerName", source = "order.buyerName")
    @Mapping(target = "recipientName", source = "order.recipientName")
    @Mapping(target = "shippingAddress", source = "order.shippingAddress")
    @Mapping(target = "shippingService", source = "order.shippingService")
    @Mapping(target = "trackingNumber", source = "order.trackingNumber")
    @Mapping(target = "estimatedDelivery", expression = "java(order.getEstimatedDelivery() == null ? null : order.getEstimatedDelivery().toString())")
    @Mapping(target = "createdAt", expression = "java(order.getCreatedAt() == null ? null : order.getCreatedAt().toString())")
    @Mapping(target = "totalAmount", source = "order.totalAmount")
    @Mapping(target = "payUrl", source = "order.payUrl")
    @Mapping(target = "recipientPhone", source = "order.recipientPhone")
    @Mapping(target = "buyerEmail", source = "order.buyerEmail")
    @Mapping(target = "refundRequested", source = "refundRequested")
    @Mapping(target = "shippingFee", source = "order.shippingFee")
    @Mapping(target = "discountAmount", source = "order.discountAmount")
    OrderResponse toResponse(Order order);

    default List<OrderResponse.Item> mapItems(List<OrderItem> items) {
        if (items == null) return List.of();
        return items.stream().map(i -> {
            OrderResponse.Item.ItemBuilder b = OrderResponse.Item.builder()
                    .productId(i.getProductId())
                    .variantId(i.getVariantId())
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .lineTotal(i.getLineTotal());

            // optional fields if exist on OrderItem
            try {
                b.name(i.getProductName());
            } catch (Throwable ignored) {
            }
            try {
                b.image(i.getImageUrl());
            } catch (Throwable ignored) {
            }
            try {
                b.size(i.getSize());
            } catch (Throwable ignored) {
            }
            try {
                b.color(i.getColor());
            } catch (Throwable ignored) {
            }

            return b.build();
        }).collect(Collectors.toList());
    }
}