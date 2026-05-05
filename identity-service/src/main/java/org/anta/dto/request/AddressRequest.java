package org.anta.dto.request;

import lombok.Data;

@Data
public class AddressRequest {

    private String detailedAddress;

    private String country;

    private String phoneNumber;

    private String recipientName;

    private String postalCode;

    private Boolean isDefault;

}
