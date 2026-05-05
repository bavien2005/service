package org.anta.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.anta.entity.Category;

import java.util.List;

@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {

    public boolean existsBySlug(String slug) {
        return count("slug", slug) > 0;
    }

    public List<Category> findAllByTitleIgnoreCase(String title, int page, int size) {
        return find("LOWER(title) = LOWER(?1)", title)
                .page(page, size)
                .list();
    }

    public long countAllByTitleIgnoreCase(String title) {
        return count("LOWER(title) = LOWER(?1)", title);
    }

    public List<Category> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(
            String name,
            String slug,
            int page,
            int size
    ) {
        String keywordName = "%" + name.toLowerCase() + "%";
        String keywordSlug = "%" + slug.toLowerCase() + "%";

        return find("LOWER(name) LIKE ?1 OR LOWER(slug) LIKE ?2", keywordName, keywordSlug)
                .page(page, size)
                .list();
    }

    public long countByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug) {
        String keywordName = "%" + name.toLowerCase() + "%";
        String keywordSlug = "%" + slug.toLowerCase() + "%";

        return count("LOWER(name) LIKE ?1 OR LOWER(slug) LIKE ?2", keywordName, keywordSlug);
    }

    public List<Category> findAllPaged(int page, int size) {
        return findAll()
                .page(page, size)
                .list();
    }
}
