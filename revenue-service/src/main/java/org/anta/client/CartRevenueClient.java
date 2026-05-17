package org.anta.client;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

@RegisterRestClient(configKey = "cart")
@Produces(MediaType.APPLICATION_JSON)
public interface CartRevenueClient {

    @GET
    @Path("/api/cart/revenue/weekly")
    List<Map<String, Object>> getExpectedRevenueWeekly();
}
