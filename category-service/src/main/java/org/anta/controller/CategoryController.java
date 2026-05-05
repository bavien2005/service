package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.request.CategoryRequest;
import org.anta.dto.response.CategoryResponse;
import org.anta.entity.Category;
import org.anta.mapper.CategoryMapper;
import org.anta.service.CategoryService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryController {

    @Inject
    CategoryService categoryService;

    @Inject
    CategoryMapper categoryMapper;

    @GET
    public Map<String, Object> list(
            @QueryParam("q") String q,
            @QueryParam("title") String title,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        return categoryService.list(q, title, page, size);
    }

    @GET
    @Path("/grouped")
    public Map<String, List<CategoryResponse>> grouped() {
        Map<String, List<Category>> grouped = categoryService.groupedByTitle();

        return grouped.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(categoryMapper::toResponse)
                                .collect(Collectors.toList())
                ));
    }

    @POST
    public CategoryResponse create(@Valid CategoryRequest rq) {
        Category saved = categoryService.create(rq);
        return categoryMapper.toResponse(saved);
    }

    @GET
    @Path("/{id}")
    public CategoryResponse getById(@PathParam("id") Long id) {
        Category category = categoryService.getById(id);
        return categoryMapper.toResponse(category);
    }

    @DELETE
    @Path("/{id}")
    public Map<String, Object> delete(@PathParam("id") Long id) {
        int deletedProducts = categoryService.deleteCategoryAndProducts(id);

        return Map.of(
                "success", true,
                "categoryId", id,
                "deletedProducts", deletedProducts
        );
    }
}