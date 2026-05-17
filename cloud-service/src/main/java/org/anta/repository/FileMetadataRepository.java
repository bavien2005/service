package org.anta.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.FileMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class FileMetadataRepository {

    @Inject
    EntityManager entityManager;

    public FileMetadata save(FileMetadata fileMetadata) {
        if (fileMetadata.getId() == null) {
            entityManager.persist(fileMetadata);
            return fileMetadata;
        }
        return entityManager.merge(fileMetadata);
    }

    public Optional<FileMetadata> findById(Long id) {
        return Optional.ofNullable(entityManager.find(FileMetadata.class, id));
    }

    public void delete(FileMetadata fileMetadata) {
        if (fileMetadata == null) {
            return;
        }

        FileMetadata managed = entityManager.contains(fileMetadata)
                ? fileMetadata
                : entityManager.merge(fileMetadata);

        entityManager.remove(managed);
    }

    public void updateProductIds(Long productId, List<Long> ids) {
        entityManager.createQuery("""
                        UPDATE FileMetadata f
                        SET f.productId = :productId
                        WHERE f.id IN :ids
                        """)
                .setParameter("productId", productId)
                .setParameter("ids", ids)
                .executeUpdate();
    }

    public List<FileMetadata> findByProductId(Long productId) {
        return entityManager.createQuery("""
                        SELECT f FROM FileMetadata f
                        WHERE f.productId = :productId
                        ORDER BY f.isMain DESC, f.id ASC
                        """, FileMetadata.class)
                .setParameter("productId", productId)
                .getResultList();
    }

    public void deleteTempFilesOlderThan(LocalDateTime cutoff) {
        entityManager.createQuery("""
                        DELETE FROM FileMetadata f
                        WHERE f.productId IS NULL
                          AND f.uploadedAt < :cutoff
                        """)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    public void resetIsMainForProduct(Long productId) {
        entityManager.createQuery("""
                        UPDATE FileMetadata f
                        SET f.isMain = false
                        WHERE f.productId = :productId
                        """)
                .setParameter("productId", productId)
                .executeUpdate();
    }

    public void clearProductForProductId(Long productId) {
        entityManager.createQuery("""
                        UPDATE FileMetadata f
                        SET f.productId = null,
                            f.isMain = false
                        WHERE f.productId = :productId
                        """)
                .setParameter("productId", productId)
                .executeUpdate();
    }

    public List<FileMetadata> findByProductIdIsNullAndUploadedAtBefore(LocalDateTime cutoff) {
        return entityManager.createQuery("""
                        SELECT f FROM FileMetadata f
                        WHERE f.productId IS NULL
                          AND f.uploadedAt < :cutoff
                        """, FileMetadata.class)
                .setParameter("cutoff", cutoff)
                .getResultList();
    }
}