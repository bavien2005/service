package org.anta.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="payment_id", nullable = false)
    private Long paymentId;

    @Column(name="event_type", nullable = false)
    private String eventType;

    @Column(columnDefinition = "json")
    private String payload;

    @Column(name="created_at", updatable = false, insertable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;
}