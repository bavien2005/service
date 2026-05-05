package org.anta.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseItemRequest {

    @NotNull
    @Min(1)
    private Integer quantity;
}
