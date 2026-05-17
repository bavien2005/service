package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.response.TopProductDTO;
import org.anta.service.DashboardCartService;

import java.util.List;

@Path("/api/cart")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardCartController {

    @Inject
    DashboardCartService dashboardService;

    @GET
    @Path("/top-products")
    public List<TopProductDTO> getTopProducts() {
        return dashboardService.getTop10Products();
    }
}