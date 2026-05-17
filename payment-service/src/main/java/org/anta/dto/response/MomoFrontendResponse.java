package org.anta.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MomoFrontendResponse {

    private String transactionId;  // requestId or txn id

    private String requestId;

    private Long amount;

    private String payUrl;

    private String deeplink;

    private String qrCodeUrl;       // URL to QR image (if momo provides)

    private String qrImageBase64;   // base64 png data (preferred)

    private Integer resultCode;

    private String message;
}

