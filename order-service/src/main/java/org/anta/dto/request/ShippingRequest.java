package org.anta.dto.request;



import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRequest {
    private String service;           // ví dụ "J&T Express"

    private String trackingNumber;    // ví dụ "JT123456789"

    private String estimatedDelivery; // yyyy-MM-dd (string) hoặc gửi ISO

}