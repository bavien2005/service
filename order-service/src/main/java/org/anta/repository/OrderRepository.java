package org.anta.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.Order;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderRepository {

    @Inject
    EntityManager entityManager;

    public Order save(Order order) {
        if (order.getId() == null) {
            entityManager.persist(order);
            return order;
        }
        return entityManager.merge(order);
    }

    public Optional<Order> findById(Long id) {
        return entityManager.createQuery("""
                        SELECT DISTINCT o FROM Order o
                        LEFT JOIN FETCH o.items
                        WHERE o.id = :id
                        """, Order.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        return entityManager.createQuery("""
                        SELECT DISTINCT o FROM Order o
                        LEFT JOIN FETCH o.items
                        WHERE o.orderNumber = :orderNumber
                        """, Order.class)
                .setParameter("orderNumber", orderNumber)
                .getResultStream()
                .findFirst();
    }

    public List<Order> findAll() {
        return entityManager.createQuery("""
                        SELECT DISTINCT o FROM Order o
                        LEFT JOIN FETCH o.items
                        ORDER BY o.id DESC
                        """, Order.class)
                .getResultList();
    }

    public void delete(Order order) {
        if (order == null) {
            return;
        }

        Order managedOrder = entityManager.contains(order)
                ? order
                : entityManager.merge(order);

        entityManager.remove(managedOrder);
    }

    public void deleteById(Long id) {
        Order order = entityManager.find(Order.class, id);
        if (order != null) {
            entityManager.remove(order);
        }
    }
}