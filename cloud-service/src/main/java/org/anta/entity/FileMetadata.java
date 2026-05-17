package org.anta.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "file_metadata")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "product_id", nullable = true)
    private Long productId;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "format")
    private String format;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "is_main")
    private Boolean isMain;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}