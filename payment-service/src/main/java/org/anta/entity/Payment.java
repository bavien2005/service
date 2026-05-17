package org.anta.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "partner_order_id", length = 200)
    private String partnerOrderId;

    @Column(name = "request_id", length = 200, unique = true)
    private String requestId;

    @Column(nullable = false)
    private Long amount;

    private String currency;

    @Column(name = "pay_url", length = 1000)
    private String payUrl;

    @Column(nullable = false)
    private String status; // PENDING, SUCCESS, FAILED

    @Column(columnDefinition = "json")
    private String extraData;

    @Column(name = "created_at", updatable = false, insertable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false,
            columnDefinition = "timestamp default current_timestamp on update current_timestamp")
    private LocalDateTime updatedAt;

}
