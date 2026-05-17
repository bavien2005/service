package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.anta.dto.response.TopProductDTO;
import org.anta.repository.CartItemsRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardCartService {

    @Inject
    CartItemsRepository cartItemsRepository;

    public List<TopProductDTO> getTop10Products() {
        List<Object[]> results = cartItemsRepository.findTop10ProductsNative();
        return results.stream()
                .map(r -> new TopProductDTO(
                        ((Number) r[0]).longValue(),   // productId
                        (String) r[1],                 // productName
                        ((Number) r[2]).longValue()    // totalQuantity
                ))
                .collect(Collectors.toList());
    }
}
