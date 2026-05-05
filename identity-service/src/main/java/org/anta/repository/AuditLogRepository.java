package org.anta.repository;


import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.anta.entity.AuditLog;

@ApplicationScoped
public class AuditLogRepository implements PanacheRepository<AuditLog> {

    @Inject
    EntityManager entityManager;

    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            persist(auditLog);
            return auditLog;
        }
        return entityManager.merge(auditLog);
    }
}