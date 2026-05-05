package org.anta.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyConfirmRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String otp;
}
