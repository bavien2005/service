package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.anta.service.PaymentService;

@Path("/api/momo")
@Slf4j
public class MomoController {

    @Inject
    PaymentService paymentService;

//    // MoMo IPN endpoint - MoMo sẽ POST form/query params or JSON
//    @POST
//    @Path("/ipn-handler")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response handleIpn(Map<String, String> jsonBody) {
//        Map<String, String> params = jsonBody;
//        log.info("[Momo IPN] Received params: {}", params);
//        boolean ok = paymentService.handleIpn(params);
//        if (ok) return Response.ok("OK").build();
//        else return Response.status(Response.Status.BAD_REQUEST).entity("INVALID").build();
//    }
}