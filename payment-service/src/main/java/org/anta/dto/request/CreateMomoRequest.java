package org.anta.dto.request;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateMomoRequest {

    private String partnerCode;

    private String requestType;

    private String ipnUrl;

    private String orderId;

    private Long amount;

    private String orderInfo;

    private String requestId;

    private String redirectUrl;

    private String lang;

    private String extraData;

    private String signature;

    private String accessKey;
}