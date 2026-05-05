package org.anta.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String title;
    private String description;
}