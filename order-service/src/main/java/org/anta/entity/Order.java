package org.anta.entity;

import jakarta.persistence.*;
import lombok.*;
import org.anta.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    // thêm orderNumber hiển thị (ví dụ ANTxxxxx hoặc partnerOrderId)
    @Column(name = "order_number", length = 100, unique = true)
    private String orderNumber;

    // buyer / recipient
    @Column(name = "buyer_name", length = 255)
    private String buyerName;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "shipping_address", length = 1000)
    private String shippingAddress;

    @Column(name = "buyer_email", length = 255)
    private String buyerEmail;

    @Column(name = "recipient_phone", length = 100)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "reservation_id")
    private Long reservationId;      // id từ product-service

    @Column(name = "pay_url", length = 1000)
    private String payUrl;           // từ payment-service (Momo)

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false,
            columnDefinition = "timestamp default current_timestamp on update current_timestamp")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "partner_order_id", length = 255)
    private String partnerOrderId;

    // helper để giữ consistency 2 chiều
    public void addItem(OrderItem item) {
        if (items == null) items = new ArrayList<>();
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        if (items != null) {
            items.remove(item);
            item.setOrder(null);
        }
    }

    @Column(name = "shipping_service", length = 255)
    private String shippingService;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "shipping_method")
    private String shippingMethod;

    @Column(name = "shipping_fee")
    private Long shippingFee;

    @Column(name = "discount_amount")
    private Long discountAmount;

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "refund_requested")
    private Boolean refundRequested;
}