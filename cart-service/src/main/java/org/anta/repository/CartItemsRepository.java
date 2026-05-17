package org.anta.repository;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.CartItems;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CartItemsRepository {

    @Inject
    EntityManager entityManager;

    public CartItems save(CartItems cartItems) {
        if (cartItems.getId() == null) {
            entityManager.persist(cartItems);
            return cartItems;
        }
        return entityManager.merge(cartItems);
    }

    public Optional<CartItems> findById(Long id) {
        return Optional.ofNullable(entityManager.find(CartItems.class, id));
    }

    public void deleteById(Long id) {
        CartItems cartItems = entityManager.find(CartItems.class, id);
        if (cartItems != null) {
            entityManager.remove(cartItems);
        }
    }

    public Optional<CartItems> findByCartIdAndProductIdAndVariantId(
            Long cartId,
            Long productId,
            Long variantId
    ) {
        return entityManager.createQuery("""
                        SELECT ci FROM CartItems ci
                        WHERE ci.cart.id = :cartId
                          AND ci.productId = :productId
                          AND (
                                (:variantId IS NULL AND ci.variantId IS NULL)
                                OR ci.variantId = :variantId
                          )
                        """, CartItems.class)
                .setParameter("cartId", cartId)
                .setParameter("productId", productId)
                .setParameter("variantId", variantId)
                .getResultStream()
                .findFirst();
    }

    public void deleteByCartId(Long cartId) {
        entityManager.createQuery("""
                        DELETE FROM CartItems ci
                        WHERE ci.cart.id = :cartId
                        """)
                .setParameter("cartId", cartId)
                .executeUpdate();
    }

    // dashboard
    @SuppressWarnings("unchecked")
    public List<Object[]> findTop10ProductsNative() {
        return entityManager.createNativeQuery("""
                        SELECT 
                            ci.product_id AS productId, 
                            ci.product_name AS productName, 
                            SUM(ci.quantity) AS totalQuantity
                        FROM cart_items ci 
                        GROUP BY ci.product_id, ci.product_name 
                        ORDER BY SUM(ci.quantity) DESC 
                        LIMIT 10
                        """)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> sumRevenueFromOpenCartsByWeek() {
        return entityManager.createNativeQuery("""
                        SELECT 
                            DATE_FORMAT(ci.created_at, '%x-W%v') AS week_label,
                            SUM(ci.quantity * ci.unit_price) AS total
                        FROM cart_items ci
                        JOIN carts c ON ci.cart_id = c.id
                        WHERE c.status = 'OPEN'
                          AND ci.created_at >= DATE_SUB(CURDATE(), INTERVAL 12 WEEK)
                        GROUP BY week_label
                        ORDER BY week_label
                        """)
                .getResultList();
    }
}