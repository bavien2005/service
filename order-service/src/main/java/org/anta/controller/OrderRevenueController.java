package org.anta.controller;


import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.response.ProductSoldQtyDTO;
import org.anta.dto.response.WeeklyRevenueDTO;
import org.anta.service.OrderRevenueService;

import java.util.List;

@Path("/api/orders/revenue")
@Produces(MediaType.APPLICATION_JSON)
public class OrderRevenueController {

    @Inject
    OrderRevenueService orderRevenueService;

    // CHỈ DÙNG ENDPOINT NÀY
    @GET
    @Path("/weekly")
    public Response getActualRevenueWeekly() {
        List<WeeklyRevenueDTO> response = orderRevenueService.getActualRevenueWeekly();
        return Response.ok(response).build();
    }

    @GET
    @Path("/products/sold-qty")
    public Response getSoldQtyByProduct() {
        List<ProductSoldQtyDTO> response = orderRevenueService.getSoldQtyByProductFromPaidOrDelivered();
        return Response.ok(response).build();
    }
}
