package org.anta.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.Payment;

import java.util.Optional;

@ApplicationScoped
public class PaymentRepository {

    @Inject
    EntityManager entityManager;

    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            entityManager.persist(payment);
            return payment;
        }
        return entityManager.merge(payment);
    }

    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Payment.class, id));
    }

    public Optional<Payment> findByRequestId(String requestId) {
        return entityManager.createQuery("""
                        SELECT p FROM Payment p
                        WHERE p.requestId = :requestId
                        """, Payment.class)
                .setParameter("requestId", requestId)
                .getResultStream()
                .findFirst();
    }

    public Optional<Payment> findByPartnerOrderId(String partnerOrderId) {
        return entityManager.createQuery("""
                        SELECT p FROM Payment p
                        WHERE p.partnerOrderId = :partnerOrderId
                        """, Payment.class)
                .setParameter("partnerOrderId", partnerOrderId)
                .getResultStream()
                .findFirst();
    }

    public Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId) {
        return entityManager.createQuery("""
                        SELECT p FROM Payment p
                        WHERE p.orderId = :orderId
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, Payment.class)
                .setParameter("orderId", orderId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    public Optional<Payment> findTopByRequestIdOrderByCreatedAtDesc(String requestId) {
        return entityManager.createQuery("""
                        SELECT p FROM Payment p
                        WHERE p.requestId = :requestId
                        ORDER BY p.createdAt DESC, p.id DESC
                        """, Payment.class)
                .setParameter("requestId", requestId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }
}