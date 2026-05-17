package org.anta.dto.request;

import org.anta.enums.Status;

import lombok.Data;

@Data
public class CartsRequest {
    private Long userId;
    private String sessionId;
    private Status status;
}
