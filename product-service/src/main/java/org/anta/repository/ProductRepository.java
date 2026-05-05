package org.anta.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.anta.entity.Product;

import java.util.List;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

    public List<Product> searchByNameIgnoreAccent(String name) {
        return list("LOWER(name) LIKE LOWER(?1)", "%" + name + "%");
    }

    public List<Product> searchFullTextLoose(String q) {
        return getEntityManager()
                .createNativeQuery("""
                    SELECT * FROM products p
                    WHERE (
                        p.name COLLATE utf8mb4_0900_ai_ci LIKE CONCAT('%', :q, '%')
                        OR p.brand COLLATE utf8mb4_0900_ai_ci LIKE CONCAT('%', :q, '%')
                        OR p.description COLLATE utf8mb4_0900_ai_ci LIKE CONCAT('%', :q, '%')
                    )
                    ORDER BY p.id DESC
                    """, Product.class)
                .setParameter("q", q)
                .getResultList();
    }

    public List<Product> findByCategoryId(Long categoryId) {
        return list("categoryId", categoryId);
    }
}