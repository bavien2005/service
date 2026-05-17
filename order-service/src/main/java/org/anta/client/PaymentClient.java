package org.anta.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.external.CreatePaymentRequest;
import org.anta.dto.external.CreatePaymentResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "payment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface PaymentClient {

    @POST
    @Path("/create")
    CreatePaymentResponse create(CreatePaymentRequest req);
}