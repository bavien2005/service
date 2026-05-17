package org.anta.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.external.ProductDto;
import org.anta.dto.external.VariantDto;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "product")
@Produces(MediaType.APPLICATION_JSON)
public interface ProductClient {

    @GET
    @Path("/api/productVariant/variant/{id}")
    VariantDto getVariant(@PathParam("id") Long id);

    // Thêm endpoint lấy product (nếu product-service hỗ trợ)
    @GET
    @Path("/api/product/{id}")
    ProductDto getProduct(@PathParam("id") Long id);
}