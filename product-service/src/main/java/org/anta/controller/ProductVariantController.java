package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.request.ProductVariantRequest;
import org.anta.dto.request.PurchaseItemRequest;
import org.anta.dto.response.ProductVariantResponse;
import org.anta.service.ProductVariantService;

import java.util.List;

@Path("/api/productVariant")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductVariantController {

    @Inject
    ProductVariantService productVariantService;

    @GET
    @Path("/listVariant/productId/{productId}")
    public List<ProductVariantResponse> getListVariantByProduct(@PathParam("productId") Long productId) {
        return productVariantService.findByProduct(productId);
    }

    @GET
    @Path("/variant/{id}")
    public ProductVariantResponse getVariant(@PathParam("id") Long id) {
        return productVariantService.getById(id);
    }

    @POST
    @Path("/add")
    public ProductVariantResponse add(ProductVariantRequest req) {
        return productVariantService.add(req);
    }

    @PUT
    @Path("/update/variantId/{id}")
    public ProductVariantResponse update(@PathParam("id") Long id, ProductVariantRequest req) {
        return productVariantService.update(id, req);
    }

    @DELETE
    @Path("/delete/variantId/{id}")
    public Response delete(@PathParam("id") Long id) {
        productVariantService.delete(id);
        return Response.ok().build();
    }

    @POST
    @Path("/reserve/id/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String reserve(
            @PathParam("id") Long id,
            @Valid PurchaseItemRequest purchaseItemRequest
    ) {
        productVariantService.reserveStock(id, purchaseItemRequest.getQuantity());
        return "reserve";
    }

    @POST
    @Path("/release/id/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String release(
            @PathParam("id") Long id,
            @Valid PurchaseItemRequest purchaseItemRequest
    ) {
        productVariantService.releaseStock(id, purchaseItemRequest.getQuantity());
        return "released";
    }
}