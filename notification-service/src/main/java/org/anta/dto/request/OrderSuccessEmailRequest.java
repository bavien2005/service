package org.anta.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSuccessEmailRequest {

    @NotBlank
    @Email
    private String to;

    @NotBlank
    private String orderNumber;

    private String customerName;

    private Long total;

    private String idempotencyKey;
}