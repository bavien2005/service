package org.anta.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NotificationEmailRequest {

    @NotBlank
    @Email
    private String to;

    @NotBlank
    private String subject;

    private String templateId;

    private Map<String, Object> templateData;

    private String rawHtml;

    private String idempotencyKey;

    private String body;
}