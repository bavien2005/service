package org.anta.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.Carts;
import org.anta.enums.Status;

import java.util.Optional;

@ApplicationScoped
public class CartsRepository {

    @Inject
    EntityManager entityManager;

    public Carts save(Carts cart) {
        if (cart.getId() == null) {
            entityManager.persist(cart);
            return cart;
        }
        return entityManager.merge(cart);
    }

    public Optional<Carts> findById(Long id) {
        return entityManager.createQuery("""
                        SELECT DISTINCT c FROM Carts c
                        LEFT JOIN FETCH c.items
                        WHERE c.id = :id
                        """, Carts.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    public Optional<Carts> findByUserIdAndStatus(Long userId, Status status) {
        return entityManager.createQuery("""
                        SELECT DISTINCT c FROM Carts c
                        LEFT JOIN FETCH c.items
                        WHERE c.userId = :userId
                          AND c.status = :status
                        """, Carts.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .getResultStream()
                .findFirst();
    }

    public Optional<Carts> findBySessionIdAndStatus(String sessionId, Status status) {
        return entityManager.createQuery("""
                        SELECT DISTINCT c FROM Carts c
                        LEFT JOIN FETCH c.items
                        WHERE c.sessionId = :sessionId
                          AND c.status = :status
                        """, Carts.class)
                .setParameter("sessionId", sessionId)
                .setParameter("status", status)
                .getResultStream()
                .findFirst();
    }

    public void delete(Carts cart) {
        if (cart == null) {
            return;
        }

        Carts managedCart = entityManager.contains(cart)
                ? cart
                : entityManager.merge(cart);

        entityManager.remove(managedCart);
    }
}