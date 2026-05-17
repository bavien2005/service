package org.anta.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "order-service")
@Produces(MediaType.APPLICATION_JSON)
public interface OrderClient {

    // Đổi trạng thái đơn hàng sang PAID
    @PUT
    @Path("/api/orders/{orderId}/paid")
    void markOrderPaid(@PathParam("orderId") Long orderId);

    // Nếu bạn muốn handle fail:
    @PUT
    @Path("/api/orders/{orderId}/payment-failed")
    void markOrderPaymentFailed(@PathParam("orderId") Long orderId);

    @GET
    @Path("/api/orders/{orderId}")
    Map<String, Object> getOrderById(@PathParam("orderId") Long orderId);
}