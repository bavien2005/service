package org.anta.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.entity.Reservation;
import org.anta.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReservationCleanupService {

    @Inject
    ReservationRepository reservationRepo;

    @Inject
    ReservationService reservationService;

    @Scheduled(every = "{app.reservation.cleanup-every}")
    @Transactional
    public void cleanupExpired() {
        List<Reservation> expired = reservationRepo.findByStatusAndExpiresAtBefore(
                "PENDING",
                LocalDateTime.now()
        );

        for (Reservation r : expired) {
            reservationService.cancelReservation(r.getId());
            r.setStatus("EXPIRED");
        }
    }
}