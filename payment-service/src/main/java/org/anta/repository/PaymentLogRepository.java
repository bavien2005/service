package org.anta.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.PaymentLog;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PaymentLogRepository {

    @Inject
    EntityManager entityManager;

    public PaymentLog save(PaymentLog paymentLog) {
        if (paymentLog.getId() == null) {
            entityManager.persist(paymentLog);
            return paymentLog;
        }
        return entityManager.merge(paymentLog);
    }

    public Optional<PaymentLog> findById(Long id) {
        return Optional.ofNullable(entityManager.find(PaymentLog.class, id));
    }

    public List<PaymentLog> findByPaymentId(Long paymentId) {
        return entityManager.createQuery("""
                        SELECT pl FROM PaymentLog pl
                        WHERE pl.paymentId = :paymentId
                        ORDER BY pl.createdAt DESC, pl.id DESC
                        """, PaymentLog.class)
                .setParameter("paymentId", paymentId)
                .getResultList();
    }
}