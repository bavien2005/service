package org.anta.client;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/product")
@RegisterRestClient(configKey = "cloud-service")
public interface CloudRestClient {

    @GET
    @Path("/{productId}")
    FileMetadataDTO[] getFilesByProductId(@PathParam("productId") Long productId);
}