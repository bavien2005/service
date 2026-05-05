package org.anta.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.anta.entity.ProductVariant;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductVariantRepository implements PanacheRepository<ProductVariant> {

    public List<ProductVariant> findByProductId(Long productId) {
        return list("product.id", productId);
    }

    public boolean existsBySku(String sku) {
        return count("sku", sku) > 0;
    }

    public Optional<ProductVariant> findBySku(String sku) {
        return find("sku", sku).firstResultOptional();
    }

    @Transactional
    public int reduceStockIfAvailable(Long id, int qty) {
        return update("stock = stock - ?1 where id = ?2 and stock >= ?1", qty, id);
    }

    @Transactional
    public int increaseStock(Long id, int qty) {
        return update("stock = stock + ?1 where id = ?2", qty, id);
    }
}