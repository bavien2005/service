package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.anta.entity.FileMetadata;
import org.anta.service.CloudService;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/cloud")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class CloudController {

    @Inject
    CloudService cloudService;

    @POST
    @Path("/upload-debug")
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadDebug(@Context HttpHeaders headers) {
        String ct = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
        log.info("DEBUG Content-Type = {}", ct);
        return Response.ok("checked").build();
    }

    @GET
    @Path("/product/{productId}")
    public Response getFilesByProductId(@PathParam("productId") Long productId) {
        List<FileMetadata> response = cloudService.getByProductId(productId);
        return Response.ok(response).build();
    }

    @POST
    @Path("/upload-multiple")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @RestForm("files") List<FileUpload> files,
            @RestForm("uploaderId") Long uploaderId) {

        if (files == null || files.isEmpty()) {
            log.info("No files received, uploaderId={}", uploaderId);
            throw new IllegalArgumentException("No files provided");
        }

        if (uploaderId == null) uploaderId = 0L; // fallback if you want

        return Response.ok(cloudService.uploadMultiple(files, uploaderId)).build();
    }

    @PUT
    @Path("/update-product/{productId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProductImages(
            @PathParam("productId") Long productId,
            Object body) {

        // body có thể là List<Long> (legacy) hoặc Map { ids: [...], mainId: ... }
        List<Long> imageIds = null;
        Long mainId = null;

        if (body == null) {
            // clear existing files for product
            cloudService.assignImagesToProduct(productId, List.of(), null);
            return Response.ok(Map.of("message", "Images unassigned")).build();
        }

        if (body instanceof List) {
            // legacy: List of ids
            @SuppressWarnings("unchecked")
            List<Object> raw = (List<Object>) body;
            imageIds = raw.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(Collectors.toList());
        } else if (body instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) body;
            Object idsObj = map.get("ids");
            if (idsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> raw = (List<Object>) idsObj;
                imageIds = raw.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(Collectors.toList());
            } else if (idsObj == null && map.get("imageIds") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> raw = (List<Object>) map.get("imageIds");
                imageIds = raw.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(Collectors.toList());
            }

            // mainId may be present
            if (map.get("mainId") != null) {
                mainId = Long.valueOf(String.valueOf(map.get("mainId")));
            } else if (map.get("main") != null) {
                mainId = Long.valueOf(String.valueOf(map.get("main")));
            }
        } else {
            // attempt to coerce via Jackson -> Map
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) body;
                Object idsObj = map.get("ids");
                if (idsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> raw = (List<Object>) idsObj;
                    imageIds = raw.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(Collectors.toList());
                }
                if (map.get("mainId") != null) mainId = Long.valueOf(String.valueOf(map.get("mainId")));
            } catch (Exception ex) {
                log.warn("Cannot parse body for update-product: {}", ex.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Invalid payload"))
                        .build();
            }
        }

        cloudService.assignImagesToProduct(productId, imageIds == null ? List.of() : imageIds, mainId);
        return Response.ok(Map.of("message", "Images linked successfully")).build();
    }

    @DELETE
    @Path("/cleanup")
    public Response cleanup() {
        cloudService.cleanUnusedFiles();
        return Response.ok(Map.of("message", "Old temp files deleted")).build();
    }

    @DELETE
    @Path("/file/{fileId}")
    public Response deleteFile(@PathParam("fileId") Long fileId) {
        cloudService.deleteFileById(fileId); // implement ở service
        return Response.ok(Map.of("message", "deleted")).build();
    }
}