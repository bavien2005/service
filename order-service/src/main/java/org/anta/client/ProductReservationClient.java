package org.anta.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.external.CreateReservationRequest;
import org.anta.dto.external.CreateReservationResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "product-reservation")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ProductReservationClient {

    @POST
    @Path("/create")
    CreateReservationResponse create(CreateReservationRequest req);

    @POST
    @Path("/id/{id}/confirm")
    void confirm(@PathParam("id") Long reservationId);

    @POST
    @Path("/id/{id}/cancel")
    void cancel(@PathParam("id") Long reservationId);
}