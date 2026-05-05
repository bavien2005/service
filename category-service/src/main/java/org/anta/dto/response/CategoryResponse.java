package org.anta.dto.response;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryResponse {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}