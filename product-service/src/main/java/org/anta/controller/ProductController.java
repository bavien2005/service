package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.request.ProductRequest;
import org.anta.dto.response.ProductResponse;
import org.anta.entity.Product;
import org.anta.service.ProductService;

import java.util.List;
import java.util.Map;

@Path("/api/product")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {

    @Inject
    ProductService productService;

    @GET
    @Path("/all")
    public List<ProductResponse> list(
            @QueryParam("title") String title,
            @QueryParam("categorySlug") String categorySlug
    ) {
        return productService.getAllFiltered(title, categorySlug);
    }

    @GET
    @Path("/{id}")
    public ProductResponse getProduct(@PathParam("id") Long id) {
        return productService.getProductById(id);
    }

    @POST
    @Path("/add")
    public ProductResponse addProduct(ProductRequest productRequest) {
        return productService.addProduct(productRequest);
    }

    @PUT
    @Path("/update/{id}")
    public ProductResponse update(@PathParam("id") Long id, ProductRequest productRequest) {
        return productService.updateProduct(id, productRequest);
    }

    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String delete(@PathParam("id") Long id) {
        productService.deleteProduct(id);
        return "Deleted id: " + id + " Successfully";
    }

    @GET
    @Path("/search/{name}")
    public List<ProductResponse> getAllProductByName(@PathParam("name") String name) {
        return productService.getProductByName(name);
    }

    @PUT
    @Path("/sync-images/{id}")
    public ProductResponse syncImages(@PathParam("id") Long id) {
        return productService.syncImagesFromCloud(id);
    }

    @PUT
    @Path("/{productId}/category/{categoryId}")
    public Product assignCategory(
            @PathParam("productId") Long productId,
            @PathParam("categoryId") Long categoryId
    ) {
        return productService.assignCategory(productId, categoryId);
    }

    @DELETE
    @Path("/{productId}/category")
    public Product removeCategory(@PathParam("productId") Long productId) {
        return productService.removeCategory(productId);
    }

    @GET
    @Path("/search")
    public List<ProductResponse> search(@QueryParam("q") String q) {
        return productService.searchProducts(q);
    }

    @GET
    @Path("/by-category/{categoryId}")
    public List<ProductResponse> byCategory(@PathParam("categoryId") Long categoryId) {
        return productService.listByCategory(categoryId);
    }

    @DELETE
    @Path("/by-category/{categoryId}")
    public Map<String, Object> deleteByCategory(@PathParam("categoryId") Long categoryId) {
        int deleted = productService.deleteProductsByCategory(categoryId);
        return Map.of(
                "success", true,
                "categoryId", categoryId,
                "deletedProducts", deleted
        );
    }
}