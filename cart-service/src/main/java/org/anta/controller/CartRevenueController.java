package org.anta.controller;


import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.response.WeeklyRevenueDTO;
import org.anta.service.CartRevenueService;

import java.util.List;

@Path("/api/cart/revenue")
@Produces(MediaType.APPLICATION_JSON)
public class CartRevenueController {

    @Inject
    CartRevenueService cartRevenueService;

    // CHỈ DÙNG ENDPOINT NÀY CHO DASHBOARD
    @GET
    @Path("/weekly")
    public List<WeeklyRevenueDTO> getExpectedRevenueWeekly() {
        return cartRevenueService.getExpectedRevenueWeekly();
    }
}