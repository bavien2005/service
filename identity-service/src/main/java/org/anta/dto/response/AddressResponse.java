package org.anta.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AddressResponse {

    private Long id;

    private String detailedAddress;

    private String country;

    private String phoneNumber;

    private String recipientName;

    private String postalCode;

    private Boolean isDefault;

    private LocalDateTime createdAt;

}
