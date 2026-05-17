package org.anta.client;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

@RegisterRestClient(configKey = "order")
@Produces(MediaType.APPLICATION_JSON)
public interface OrderRevenueClient {

    @GET
    @Path("/api/orders/revenue/weekly")
    List<Map<String, Object>> getActualRevenueWeekly();
}
