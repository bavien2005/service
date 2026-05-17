package org.anta.client;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "product-service")
public interface ProductRestClient {

    @GET
    @Path("/{productId}")
    ProductDTO getProductById(@PathParam("productId") Long productId);
}