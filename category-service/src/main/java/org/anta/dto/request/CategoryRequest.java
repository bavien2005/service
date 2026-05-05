package org.anta.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "slug is required")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be kebab-case")
    private String slug;

    @Size(max = 500, message = "description max 500 chars")
    private String description;

    @NotBlank(message = "title is required")
    private String title;
}
