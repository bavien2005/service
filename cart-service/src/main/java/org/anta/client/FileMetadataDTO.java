package org.anta.client;


import lombok.Data;

@Data
public class FileMetadataDTO {
    private Long id;
    private Long productId;
    private String url;
    private boolean isMain;   // true nếu là ảnh chính
    private Long uploaderId;
    private String contentType;
    private Long size;
}
