package org.anta.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.request.CreateOrderRequest;
import org.anta.dto.request.ShippingRequest;
import org.anta.dto.response.CreateOrderResponse;
import org.anta.dto.response.OrderResponse;
import org.anta.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/api/orders")
@Consumes(MediaType.APPLICATION_JSON)
@jakarta.ws.rs.Produces(MediaType.APPLICATION_JSON)
public class OrderController {

    @Inject
    OrderService orderService;

    private Logger log = Logger.getLogger(OrderController.class.getName());

    @POST
    @Path("/create")
    public Response create(CreateOrderRequest req) {
        try {
            log.info("incoming createOrder request: " +
                    new ObjectMapper().writeValueAsString(req));
        } catch (JsonProcessingException e) {
            log.warning("Cannot serialize CreateOrderRequest: " + e.getMessage());
        }
        CreateOrderResponse response = orderService.createOrder(req);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        OrderResponse response = orderService.get(id);
        return Response.ok(response).build();
    }

    // Endpoint để payment-service gọi về sau IPN
    @POST
    @Path("/{id}/payment-status/{status}")
    public Response updatePayment(
            @PathParam("id") Long id,
            @PathParam("status") String status
    ) {
        orderService.updatePaymentStatus(id, status);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") Long id) {
        orderService.cancelOrder(id);
        return Response.ok().build();
    }

    // --- trong cùng class OrderController ---
    @POST
    @Path("/{id}/payment-callback")
    public Response paymentCallback(
            @PathParam("id") Long id,
            Map<String, Object> body
    ) {
        String status = body.get("status") != null ? body.get("status").toString() : null;
        Object paymentIdObj = body.get("paymentId");
        Object requestIdObj = body.get("requestId");

        log.info("Payment callback for orderId= " + id + "body={}" + body);

        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "status required"))
                    .build();
        }

        // call existing service method
        orderService.updatePaymentStatus(id, status);

        // (Optional) you could record paymentId/requestId in order history if needed
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/paid")
    public Response markPaid(@PathParam("id") Long id) {
        orderService.markAsPaid(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/payment-failed")
    public Response markPaymentFailed(@PathParam("id") Long id) {
        orderService.markPaymentFailed(id);
        return Response.noContent().build();
    }

    // 1) List orders (GET /api/orders) — optional query params: search, status, orderNumber
    @GET
    public Response list(
            @QueryParam("userId") Long userId,   // ✅ thêm
            @QueryParam("search") String search,
            @QueryParam("status") String status,
            @QueryParam("orderNumber") String orderNumber
    ) {
        List<OrderResponse> list = orderService.findOrders(userId, search, status, orderNumber)
                .stream().map(orderService::toResponse).collect(Collectors.toList());
        return Response.ok(list).build();
    }

    // 2) Update generic order status (PUT /api/orders/{id}/status)
    @PUT
    @Path("/{id}/status")
    public Response updateStatus(
            @PathParam("id") Long id,
            Map<String, String> body
    ) {
        String status = body.get("status");
        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "status required"))
                    .build();
        }
        orderService.updateStatus(id, status);
        return Response.ok(Map.of("message", "Status updated")).build();
    }

    // 3) Arrange shipping (PUT /api/orders/{id}/shipping)
    @PUT
    @Path("/{id}/shipping")
    public Response arrangeShipping(
            @PathParam("id") Long id,
            ShippingRequest req
    ) {
        orderService.arrangeShipping(id, req);
        return Response.ok(Map.of("message", "Shipping scheduled")).build();
    }

    // OrderController.java
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        Map<String, Object> resp = orderService.adminDeleteOrRefund(id);
        // resp: { deleted: true/false, refundRequested: true/false, message: ... }
        return Response.ok(resp).build();
    }

    @POST
    @Path("/{id}/cancel-admin")
    public Response cancelAdmin(@PathParam("id") Long id) {
        Map<String, Object> resp = orderService.adminCancelOrRefund(id);
        return Response.ok(resp).build();
    }
}