package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.request.CreateReservationRequest;
import org.anta.dto.response.CreateReservationResponse;
import org.anta.entity.Reservation;
import org.anta.service.ReservationService;

@Path("/api/reservations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservationController {

    @Inject
    ReservationService reservationService;

    @POST
    @Path("/create")
    public CreateReservationResponse create(@Valid CreateReservationRequest req) {
        Reservation res = reservationService.createReservation(req);

        CreateReservationResponse dto = new CreateReservationResponse();
        dto.setReservationId(res.getId());
        dto.setStatus(res.getStatus());

        return dto;
    }

    @POST
    @Path("/id/{id}/confirm")
    public Response confirm(@PathParam("id") Long id) {
        reservationService.confirmReservation(id);
        return Response.ok().build();
    }

    @POST
    @Path("/id/{id}/cancel")
    public Response cancel(@PathParam("id") Long id) {
        reservationService.cancelReservation(id);
        return Response.ok().build();
    }
}