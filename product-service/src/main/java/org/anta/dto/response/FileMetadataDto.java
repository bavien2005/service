package org.anta.dto.response;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class FileMetadataDto {

    private Long id;

    private String url;

    private Long uploaderId;

    private Long productId;

    private String publicId;

    private String format;

    private String resourceType;

    private LocalDateTime uploadedAt;

    private Boolean isMain;
}
