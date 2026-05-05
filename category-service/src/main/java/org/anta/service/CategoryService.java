package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.client.ProductClient;
import org.anta.dto.request.CategoryRequest;
import org.anta.entity.Category;
import org.anta.exception.ConflictException;
import org.anta.exception.NotFoundException;
import org.anta.mapper.CategoryMapper;
import org.anta.repository.CategoryRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class CategoryService {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    CategoryMapper categoryMapper;

    @Inject
    ProductClient productClient;

    @Transactional
    public Category create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("Slug already exists");
        }

        Category category = categoryMapper.toEntity(request);

        categoryRepository.persist(category);

        return category;
    }

    @Transactional
    public Map<String, Object> list(String q, String title, int page, int size) {
        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 20;
        }

        List<Category> content;
        long totalElements;

        if (title != null && !title.isBlank()) {
            content = categoryRepository.findAllByTitleIgnoreCase(title, page, size);
            totalElements = categoryRepository.countAllByTitleIgnoreCase(title);
        } else if (q != null && !q.isBlank()) {
            content = categoryRepository.findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(q, q, page, size);
            totalElements = categoryRepository.countByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(q, q);
        } else {
            content = categoryRepository.findAllPaged(page, size);
            totalElements = categoryRepository.count();
        }

        long totalPages = size == 0 ? 0 : (long) Math.ceil((double) totalElements / size);

        return Map.of(
                "content", content.stream().map(categoryMapper::toResponse).toList(),
                "page", page,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages
        );
    }

    @Transactional
    public Map<String, List<Category>> groupedByTitle() {
        return categoryRepository.listAll().stream()
                .collect(Collectors.groupingBy(c ->
                        c.getTitle() == null ? "" : c.getTitle().toLowerCase()
                ));
    }

    @Transactional
    public Category getById(Long id) {
        return categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }

    @Transactional
    public int deleteCategoryAndProducts(Long categoryId) {
        Category category = categoryRepository.findByIdOptional(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));

        int deletedProducts = productClient.deleteProductsByCategory(categoryId);

        categoryRepository.delete(category);

        return deletedProducts;
    }
}