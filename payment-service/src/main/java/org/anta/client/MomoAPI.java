package org.anta.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.request.CreateMomoRequest;
import org.anta.dto.response.CreateMomoResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "momo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface MomoAPI {

    @POST
    @Path("/create")
    CreateMomoResponse createMomoQR(CreateMomoRequest request);

//    @GET
//    @Path("/ipn-handler")
//    String handleIPN();
}