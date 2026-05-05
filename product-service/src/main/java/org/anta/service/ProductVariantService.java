package org.anta.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.dto.request.ProductVariantRequest;
import org.anta.dto.response.ProductVariantResponse;
import org.anta.entity.Product;
import org.anta.entity.ProductVariant;
import org.anta.exception.InsufficientStockException;
import org.anta.mapper.ProductVariantMapper;
import org.anta.repository.ProductRepository;
import org.anta.repository.ProductVariantRepository;

import java.util.List;

@ApplicationScoped
public class ProductVariantService {

    @Inject
    ProductVariantRepository productVariantRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductVariantMapper productVariantMapper;

    @Transactional
    public List<ProductVariantResponse> findByProduct(Long productId) {
        return productVariantMapper.toResponseList(productVariantRepository.findByProductId(productId));
    }

    @Transactional
    public ProductVariantResponse getById(Long id) {
        ProductVariant v = productVariantRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        return productVariantMapper.toResponse(v);
    }

    @Transactional
    public ProductVariantResponse add(ProductVariantRequest req) {
        Product product = productRepository.findByIdOptional(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (req.getSku() != null && productVariantRepository.existsBySku(req.getSku())) {
            throw new RuntimeException("SKU already exists");
        }

        ProductVariant entity = productVariantMapper.toEntity(req);
        entity.setProduct(product);

        productVariantRepository.persist(entity);

        return productVariantMapper.toResponse(entity);
    }

    @Transactional
    public ProductVariantResponse update(Long id, ProductVariantRequest req) {
        ProductVariant existing = productVariantRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        if (req.getProductId() != null && !req.getProductId().equals(existing.getProduct().getId())) {
            Product newProduct = productRepository.findByIdOptional(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            existing.setProduct(newProduct);
        }

        productVariantMapper.updateFromRequest(req, existing);

        return productVariantMapper.toResponse(existing);
    }

    @Transactional
    public void delete(Long id) {
        boolean deleted = productVariantRepository.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Variant not found");
        }
    }

    @Transactional
    public void reserveStock(Long variantId, int qty) {
        int updated = productVariantRepository.reduceStockIfAvailable(variantId, qty);
        if (updated == 0) {
            throw new InsufficientStockException("Not enough stock for variant id " + variantId);
        }
    }

    @Transactional
    public void releaseStock(Long variantId, int qty) {
        productVariantRepository.increaseStock(variantId, qty);
    }
}