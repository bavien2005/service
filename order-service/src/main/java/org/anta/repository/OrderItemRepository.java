package org.anta.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.OrderItem;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrderItemRepository {

    @Inject
    EntityManager entityManager;

    public OrderItem save(OrderItem orderItem) {
        if (orderItem.getId() == null) {
            entityManager.persist(orderItem);
            return orderItem;
        }
        return entityManager.merge(orderItem);
    }

    public Optional<OrderItem> findById(Long id) {
        return Optional.ofNullable(entityManager.find(OrderItem.class, id));
    }

    public void deleteById(Long id) {
        OrderItem orderItem = entityManager.find(OrderItem.class, id);
        if (orderItem != null) {
            entityManager.remove(orderItem);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> sumWeeklyRevenueFromCompletedOrders() {
        return entityManager.createNativeQuery("""
                        SELECT 
                          CONCAT(
                            YEAR(o.created_at),
                            '-W',
                            LPAD(WEEK(o.created_at, 1), 2, '0')
                          ) AS week,
                          SUM(oi.quantity * oi.unit_price) AS revenue
                        FROM order_items oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE o.status IN ('PAID','DELIVERED')
                        GROUP BY 
                          CONCAT(
                            YEAR(o.created_at),
                            '-W',
                            LPAD(WEEK(o.created_at, 1), 2, '0')
                          )
                        ORDER BY 
                          CONCAT(
                            YEAR(o.created_at),
                            '-W',
                            LPAD(WEEK(o.created_at, 1), 2, '0')
                          )
                        """)
                .getResultList();
    }

    public void deleteByOrderId(Long orderId) {
        entityManager.createQuery("""
                        DELETE FROM OrderItem oi
                        WHERE oi.order.id = :orderId
                        """)
                .setParameter("orderId", orderId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> sumSoldQtyByProductFromPaidOrDelivered() {
        return entityManager.createNativeQuery("""
                        SELECT 
                          oi.product_id AS productId,
                          COALESCE(SUM(oi.quantity), 0) AS soldQty
                        FROM order_items oi
                        JOIN orders o ON oi.order_id = o.id
                        WHERE o.status IN ('PAID','DELIVERED')
                          AND oi.product_id IS NOT NULL
                        GROUP BY oi.product_id
                        ORDER BY soldQty DESC
                        """)
                .getResultList();
    }
}