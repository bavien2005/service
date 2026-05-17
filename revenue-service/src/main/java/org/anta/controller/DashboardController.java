package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.anta.dto.WeeklyRevenueComparisonDTO;
import org.anta.service.DashboardService;

import java.util.List;

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardController {

    @Inject
    DashboardService dashboardService;

    // Endpoint cho FE vẽ biểu đồ
    @GET
    @Path("/revenue/weekly")
    public List<WeeklyRevenueComparisonDTO> getWeeklyRevenueComparison() {
        return dashboardService.getWeeklyRevenueComparison();
    }
}