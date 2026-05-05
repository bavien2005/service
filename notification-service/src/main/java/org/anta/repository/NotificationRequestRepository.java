package org.anta.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.NotificationRequestEntity;

import java.util.Optional;

@ApplicationScoped
public class NotificationRequestRepository implements PanacheRepositoryBase<NotificationRequestEntity, String> {

    @Inject
    EntityManager entityManager;

    public Optional<NotificationRequestEntity> findByIdempotencyKey(String idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional();
    }

    public NotificationRequestEntity save(NotificationRequestEntity entity) {
        if (entity.getId() == null) {
            persist(entity);
            return entity;
        }

        NotificationRequestEntity existing = findById(entity.getId());

        if (existing == null) {
            persist(entity);
            return entity;
        }

        return entityManager.merge(entity);
    }
}