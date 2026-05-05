package org.anta.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.dto.request.CreateReservationRequest;
import org.anta.entity.Reservation;
import org.anta.entity.ReservationItem;
import org.anta.exception.ReservationException;
import org.anta.repository.ProductVariantRepository;
import org.anta.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReservationService {

    @Inject
    ReservationRepository reservationRepo;

    @Inject
    ProductVariantRepository variantRepo;

    private final int DEFAULT_TTL_SECONDS = 15 * 60;

    @Transactional
    public Reservation createReservation(CreateReservationRequest req) {
        String requestId = req.getRequestId();

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        var existing = reservationRepo.findByRequestId(requestId);
        if (existing.isPresent()) {
            return existing.get();
        }

        int ttl = req.getTtlSeconds() != null ? req.getTtlSeconds() : DEFAULT_TTL_SECONDS;
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttl);

        Reservation res = Reservation.builder()
                .requestId(requestId)
                .status("PENDING")
                .expiresAt(expiresAt)
                .build();

        List<ReservationItem> items = new ArrayList<>();

        for (CreateReservationRequest.ReservationLine line : req.getItems()) {
            int updated = variantRepo.reduceStockIfAvailable(line.getVariantId(), line.getQuantity());

            if (updated == 0) {
                throw new ReservationException("Not enough stock for variant " + line.getVariantId());
            }

            ReservationItem ri = ReservationItem.builder()
                    .reservation(res)
                    .variantId(line.getVariantId())
                    .quantity(line.getQuantity())
                    .build();

            items.add(ri);
        }

        res.setItems(items);
        reservationRepo.persist(res);

        return res;
    }

    @Transactional
    public void confirmReservation(Long reservationId) {
        Reservation res = reservationRepo.findByIdOptional(reservationId)
                .orElseThrow(() -> new ReservationException("Reservation not found"));

        if (!"PENDING".equals(res.getStatus())) {
            throw new ReservationException("Cannot confirm reservation in state: " + res.getStatus());
        }

        res.setStatus("CONFIRMED");
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation res = reservationRepo.findByIdOptional(reservationId)
                .orElseThrow(() -> new ReservationException("Reservation not found"));

        if ("CANCELLED".equals(res.getStatus()) || "EXPIRED".equals(res.getStatus())) {
            return;
        }

        res.getItems().forEach(item -> variantRepo.increaseStock(item.getVariantId(), item.getQuantity()));

        res.setStatus("CANCELLED");
    }
}