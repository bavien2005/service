package org.anta.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.anta.client.PaymentClient;
import org.anta.client.ProductClient;
import org.anta.client.ProductReservationClient;
import org.anta.dto.external.CreatePaymentRequest;
import org.anta.dto.external.CreatePaymentResponse;
import org.anta.dto.external.CreateReservationRequest;
import org.anta.dto.external.CreateReservationResponse;
import org.anta.dto.external.ProductDto;
import org.anta.dto.external.ReservationItem;
import org.anta.dto.external.VariantDto;
import org.anta.dto.request.CreateOrderRequest;
import org.anta.dto.request.OrderItemRequest;
import org.anta.dto.request.ShippingRequest;
import org.anta.dto.response.CreateOrderResponse;
import org.anta.dto.response.OrderResponse;
import org.anta.entity.Order;
import org.anta.entity.OrderItem;
import org.anta.enums.OrderStatus;
import org.anta.mapper.OrderMapper;
import org.anta.repository.OrderItemRepository;
import org.anta.repository.OrderRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@Slf4j
public class OrderService {

    @Inject
    OrderRepository orderRepo;

    @Inject
    OrderItemRepository itemRepo;

    @Inject
    @RestClient
    ProductReservationClient reservationClient;

    @Inject
    @RestClient
    PaymentClient paymentClient;

    @Inject
    OrderMapper mapper;

    @Inject
    @RestClient
    ProductClient productClient;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items required");
        }

        log.info("Creating order for userId={}, itemsCount={}", req.getUserId(), req.getItems().size());

        long shippingFeeVnd = req.getShippingFee() != null ? req.getShippingFee() : 0L;
        long discountVnd =
                req.getDiscountAmount() != null ? req.getDiscountAmount()
                        : (req.getDiscount() != null ? req.getDiscount().longValue() : 0L);

        // 1) compute total & gather variant/product metadata (cache to avoid duplicate calls)
        BigDecimal total = BigDecimal.ZERO;
        Map<Long, VariantDto> variantCache = new java.util.HashMap<>();
        Map<Long, ProductDto> productCache = new java.util.HashMap<>();

        for (OrderItemRequest it : req.getItems()) {
            if (it.getVariantId() == null) {
                throw new IllegalArgumentException("variantId required for each item");
            }

            VariantDto variant = variantCache.computeIfAbsent(it.getVariantId(), id -> productClient.getVariant(id));

            if (variant == null || variant.getPrice() == null) {
                throw new RuntimeException("Variant price not found for variantId=" + it.getVariantId());
            }

            BigDecimal unit = variant.getPrice();
            int qty = (it.getQuantity() == null) ? 0 : it.getQuantity();
            BigDecimal line = unit.multiply(BigDecimal.valueOf(qty));
            total = total.add(line);
        }

        // 2) create order record (with total) and set buyer/recipient info from request
        String orderNumberProvided = req.getOrderNumber();
        String generatedOrderNumber = (orderNumberProvided != null && !orderNumberProvided.isBlank())
                ? orderNumberProvided
                : ("ANT" + String.valueOf(System.currentTimeMillis())
                .substring(Math.max(0, String.valueOf(System.currentTimeMillis()).length() - 8)));

        // total hiện tại đang là subtotal items
        BigDecimal subtotal = total;
        BigDecimal totalPayable = subtotal
                .subtract(BigDecimal.valueOf(discountVnd))
                .add(BigDecimal.valueOf(shippingFeeVnd));

        if (totalPayable.signum() < 0) totalPayable = BigDecimal.ZERO;

        Order order = Order.builder()
                .userId(req.getUserId())
                .orderNumber(generatedOrderNumber)
                .buyerName(req.getBuyerName())
                .recipientName(req.getRecipientName())
                .recipientPhone(req.getRecipientPhone())
                .buyerEmail(req.getEmail())
                .shippingAddress(req.getShippingAddress())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(totalPayable)
                .paymentMethod(req.getPaymentMethod())
                .shippingMethod(req.getShippingMethod())
                .shippingFee(shippingFeeVnd)
                .discountAmount(discountVnd)
                .promoCode(req.getPromoCode())
                .build();

        order = orderRepo.save(order);
        log.info("Saved order id={} orderNumber={}", order.getId(), order.getOrderNumber());

        // ensure items list initialized
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }

        // 3) save items (create OrderItem entities with product metadata)
        for (OrderItemRequest it : req.getItems()) {
            VariantDto variant = variantCache.get(it.getVariantId());

            if (variant == null) {
                variant = productClient.getVariant(it.getVariantId());
                if (variant == null) {
                    throw new RuntimeException("Variant not found for id=" + it.getVariantId());
                }
                variantCache.put(it.getVariantId(), variant);
            }

            String productName = null;
            String imageUrl = null;
            String size = null;
            String color = null;
            Long productId = variant.getProductId();

            // attempt to read fields from VariantDto (you extended it)
            try {
                productName = variant.getProductName() != null ? variant.getProductName() : variant.getName();
            } catch (Exception ignored) {
            }

            try {
                imageUrl = variant.getImageUrl() != null ? variant.getImageUrl() : variant.getThumbnail();
            } catch (Exception ignored) {
            }

            try {
                size = variant.getSize();
            } catch (Exception ignored) {
            }

            try {
                color = variant.getColor();
            } catch (Exception ignored) {
            }

            // fallback: if missing info and productId available, fetch product
            if ((productName == null || imageUrl == null) && productId != null) {
                ProductDto prod = productCache.computeIfAbsent(productId, id -> {
                    try {
                        return productClient.getProduct(id);
                    } catch (Exception ex) {
                        log.warn("productClient.getProduct failed for id={}", id, ex);
                        return null;
                    }
                });

                if (prod != null) {
                    if (productName == null) productName = prod.getName();
                    if (imageUrl == null) imageUrl = prod.getThumbnail();
                }
            }

            BigDecimal unitPrice = variant.getPrice();
            int qty = (it.getQuantity() == null) ? 0 : it.getQuantity();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .productId(productId)
                    .variantId(it.getVariantId())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .productName(productName)
                    .imageUrl(imageUrl)
                    .size(size)
                    .color(color)
                    .build();

            // persist item (cascade could save automatically, but explicit save is safer)
            oi.setOrder(order);
            itemRepo.save(oi);
            order.getItems().add(oi);

            log.debug("Saved orderItem variantId={} qty={} unitPrice={} productName={}",
                    oi.getVariantId(), oi.getQuantity(), oi.getUnitPrice(), oi.getProductName());
        }

        BigDecimal recomputedSubtotal = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal recomputedTotal = recomputedSubtotal
                .subtract(BigDecimal.valueOf(discountVnd))
                .add(BigDecimal.valueOf(shippingFeeVnd));

        if (recomputedTotal.signum() < 0) recomputedTotal = BigDecimal.ZERO;

        order.setShippingFee(shippingFeeVnd);
        order.setDiscountAmount(discountVnd);
        order.setTotalAmount(recomputedTotal);
        orderRepo.save(order);

        Long reservationId = null;

        try {
            CreateReservationRequest r = CreateReservationRequest.builder()
                    .userId(req.getUserId())
                    .ttlSeconds(900)
                    .items(req.getItems().stream()
                            .map(i -> ReservationItem.builder()
                                    .variantId(i.getVariantId())
                                    .quantity(i.getQuantity())
                                    .build())
                            .toList())
                    .build();

            log.info("Calling reservation service for orderId={} ...", order.getId());

            CreateReservationResponse rResp = reservationClient.create(r);
            reservationId = rResp.getReservationId();
            order.setReservationId(reservationId);
            orderRepo.save(order);

            log.info("Reservation created id={} for orderId={}", reservationId, order.getId());
        } catch (Exception ex) {
            log.error("Failed to create reservation for orderId={}", order.getId(), ex);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepo.save(order);
            throw new RuntimeException("Reservation service error: " + ex.getMessage(), ex);
        }

        try {
            BigDecimal totalAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            long amountVnd = totalAmount.setScale(0, RoundingMode.HALF_UP).longValue();

            log.info("Calling payment service for orderId={}, amount={}", order.getId(), amountVnd);

            CreatePaymentResponse pay = paymentClient.create(CreatePaymentRequest.builder()
                    .orderId(order.getId())
                    .userId(req.getUserId())
                    .amount(amountVnd)
                    .build());

            if (pay == null) {
                log.error("Payment service returned null for orderId={}", order.getId());

                if (reservationId != null) {
                    try {
                        reservationClient.cancel(reservationId);
                    } catch (Exception e) {
                        log.warn("Cancel reservation failed", e);
                    }
                }

                order.setStatus(OrderStatus.FAILED);
                orderRepo.save(order);
                throw new RuntimeException("Payment service returned null");
            }

            Integer resultCode = null;

            try {
                resultCode = pay.getResultCode();
            } catch (Exception ignored) {
            }

            if ((resultCode != null && resultCode != 0) || pay.getPayUrl() == null) {
                log.error("Payment creation failed for orderId={}, resultCode={}, message={}, resp={}",
                        order.getId(), resultCode, pay.getMessage(), pay);

                if (reservationId != null) {
                    try {
                        reservationClient.cancel(reservationId);
                    } catch (Exception e) {
                        log.warn("Cancel reservation failed", e);
                    }
                }

                order.setStatus(OrderStatus.FAILED);
                orderRepo.save(order);
                throw new RuntimeException("Payment was not created: " + (pay.getMessage() == null ? "unknown" : pay.getMessage()));
            }

            // success: save payUrl onto order
            order.setPayUrl(pay.getPayUrl());
            orderRepo.save(order);

            log.info("Payment created payUrl={} for orderId={}", pay.getPayUrl(), order.getId());

        } catch (Exception ex) {
            log.error("Payment service failed for orderId={}. Will try to cancel reservation {}", order.getId(), reservationId, ex);

            if (reservationId != null) {
                try {
                    reservationClient.cancel(reservationId);
                    log.info("Cancelled reservation id={} after payment failure", reservationId);
                } catch (Exception e2) {
                    log.warn("Failed to cancel reservation id={} after payment failure", reservationId, e2);
                }
            }

            order.setStatus(OrderStatus.FAILED);
            orderRepo.save(order);
            throw new RuntimeException("Payment service error: " + ex.getMessage(), ex);
        }

        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .payUrl(order.getPayUrl())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .buyerName(order.getBuyerName())
                .email(order.getBuyerEmail())
                .orderNumber(order.getOrderNumber())
                .total(order.getTotalAmount() == null ? null : order.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue())
                .shippingFee(order.getShippingFee())
                .build();
    }

    @Transactional
    public OrderResponse get(Long id) {
        Order o = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        return mapper.toResponse(o);
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, String paymentStatus) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
            // xác nhận reservation để trừ tồn
            if (o.getReservationId() != null) {
                reservationClient.confirm(o.getReservationId());
            }
            o.setStatus(OrderStatus.PAID);
        } else {
            // huỷ reservation, trả hàng về kho
            if (o.getReservationId() != null) {
                reservationClient.cancel(o.getReservationId());
            }
            o.setStatus("FAILED".equalsIgnoreCase(paymentStatus) ? OrderStatus.FAILED : OrderStatus.CANCELLED);
        }

        orderRepo.save(o);
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order o = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));

        if (o.getStatus() == OrderStatus.PAID) return; // đã thanh toán thì không huỷ

        if (o.getReservationId() != null) {
            reservationClient.cancel(o.getReservationId());
        }

        o.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(o);
    }

    @Transactional
    public void markAsPaid(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        // confirm reservation giống như updatePaymentStatus("SUCCESS")
        if (order.getReservationId() != null) {
            try {
                reservationClient.confirm(order.getReservationId());
            } catch (Exception ex) {
                log.warn("Failed to confirm reservation {} for order {} when marking PAID",
                        order.getReservationId(), orderId, ex);
            }
        }

        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    @Transactional
    public void markPaymentFailed(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        order.setStatus(OrderStatus.FAILED); // nếu enum có
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    @Transactional
    public List<Order> findOrders(Long userId, String search, String status, String orderNumber) {
        List<Order> all = orderRepo.findAll();
        Stream<Order> s = all.stream();

        if (userId != null) {
            s = s.filter(o -> o.getUserId() != null && o.getUserId().equals(userId));
        }

        // giữ nguyên filter cũ của bạn
        if (orderNumber != null && !orderNumber.isBlank()) {
            s = s.filter(o -> orderNumber.equals(String.valueOf(o.getOrderNumber())) ||
                    (o.getPartnerOrderId() != null && o.getPartnerOrderId().contains(orderNumber)));
        } else if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();

            s = s.filter(o -> (o.getOrderNumber() != null && o.getOrderNumber().toLowerCase().contains(q))
                    || (o.getBuyerName() != null && o.getBuyerName().toLowerCase().contains(q))
                    || (o.getRecipientName() != null && o.getRecipientName().toLowerCase().contains(q))
                    || (o.getShippingAddress() != null && o.getShippingAddress().toLowerCase().contains(q)));
        }

        if (status != null && !status.isBlank()) {
            try {
                OrderStatus st = OrderStatus.valueOf(status.toUpperCase());
                s = s.filter(o -> o.getStatus() == st);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return s.collect(Collectors.toList());
    }

    public OrderResponse toResponse(Order o) {
        // prefer using your mapper if present
        if (mapper != null) {
            return mapper.toResponse(o);
        }

        // fallback manual map
        OrderResponse resp = OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .status(o.getStatus() == null ? null : o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .payUrl(o.getPayUrl())
                .shippingFee(o.getShippingFee())
                .discountAmount(o.getDiscountAmount())
                .items(o.getItems().stream().map(it -> OrderResponse.Item.builder()
                                .productId(it.getProductId())
                                .variantId(it.getVariantId())
                                .quantity(it.getQuantity())
                                .unitPrice(it.getUnitPrice())
                                .lineTotal(it.getLineTotal())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        return resp;
    }

    @Transactional
    public void updateStatus(Long orderId, String statusStr) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
            o.setStatus(newStatus);
            o.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(o);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid status: " + statusStr);
        }
    }

    @Transactional
    public void arrangeShipping(Long orderId, ShippingRequest req) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        // set shipping info (assumes Order entity has fields trackingNumber, shippingService, estimatedDelivery)
        o.setTrackingNumber(req.getTrackingNumber());
        o.setShippingService(req.getService());

        if (req.getEstimatedDelivery() != null) {
            try {
                o.setEstimatedDelivery(LocalDate.parse(req.getEstimatedDelivery()));
            } catch (Exception ex) {
            }
        }

        if (o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.PENDING_PAYMENT) {
            o.setStatus(OrderStatus.CONFIRMED);
        } else if (o.getStatus() == OrderStatus.PAID) {
            o.setStatus(OrderStatus.SHIPPED);
        }

        o.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(o);
    }

    @Transactional
    public Map<String, Object> adminCancelOrRefund(Long id) {
        Order o = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));

        // PAID -> không cancel kiểu thường, mà chuyển CANCELLED + refundRequested=true
        if (o.getStatus() == OrderStatus.PAID) {
            o.setStatus(OrderStatus.CANCELLED);
            o.setRefundRequested(true);
            o.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(o);

            return Map.of(
                    "deleted", false,
                    "refundRequested", true,
                    "message", "Đã ghi nhận yêu cầu hoàn lại tiền"
            );
        }

        // chưa PAID -> cancel bình thường + trả reservation về kho
        if (o.getReservationId() != null) {
            reservationClient.cancel(o.getReservationId());
        }

        o.setStatus(OrderStatus.CANCELLED);
        o.setRefundRequested(false);
        o.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(o);

        return Map.of(
                "deleted", false,
                "refundRequested", false,
                "message", "Đã hủy đơn hàng"
        );
    }

    @Transactional
    public Map<String, Object> adminDeleteOrRefund(Long id) {
        Order o = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));

        // PAID -> không hard delete, chuyển sang refund request
        if (o.getStatus() == OrderStatus.PAID) {
            o.setStatus(OrderStatus.CANCELLED);
            o.setRefundRequested(true);
            o.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(o);

            return Map.of(
                    "deleted", false,
                    "refundRequested", true,
                    "message", "Đơn đã thanh toán: chuyển sang yêu cầu hoàn lại tiền (không xóa dữ liệu)"
            );
        }

        if (o.getReservationId() != null) {
            try {
                reservationClient.cancel(o.getReservationId());
            } catch (Exception ignored) {
            }
        }

        itemRepo.deleteByOrderId(o.getId());
        orderRepo.delete(o);

        return Map.of(
                "deleted", true,
                "refundRequested", false,
                "message", "Đã xóa đơn hàng"
        );
    }
}