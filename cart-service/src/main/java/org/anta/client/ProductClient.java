package org.anta.client;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ProductClient {

    @Inject
    @RestClient
    ProductRestClient productRestClient;

    public ProductDTO getProductById(Long productId) {
        return productRestClient.getProductById(productId);
    }
}
