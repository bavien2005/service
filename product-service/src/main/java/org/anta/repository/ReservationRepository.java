package org.anta.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.anta.entity.Reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ReservationRepository implements PanacheRepository<Reservation> {

    public Optional<Reservation> findByRequestId(String requestId) {
        return find("requestId", requestId).firstResultOptional();
    }

    public List<Reservation> findByStatusAndExpiresAtBefore(String status, LocalDateTime time) {
        return list("status = ?1 and expiresAt < ?2", status, time);
    }
}