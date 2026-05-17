package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.dto.response.CreateMomoResponse;
import org.anta.dto.response.PaymentStatusResponse;
import org.anta.service.PaymentService;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Path("/api/payments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentController {

    @Inject
    PaymentService paymentService;

    private Logger logger = Logger.getLogger(PaymentController.class.getName());

    @POST
    @Path("/create")
    public Response create(Map<String, Object> body) {
        // Lấy orderId (có thể null)
        Long orderId = null;
        Object orderIdObj = body.get("orderId");
        if (orderIdObj != null && !"null".equals(orderIdObj.toString())) {
            try {
                orderId = Long.valueOf(orderIdObj.toString());
            } catch (NumberFormatException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid orderId")
                        .type(MediaType.TEXT_PLAIN)
                        .build();
            }
        }

        // Lấy userId (có thể null)
        Long userId = null;
        Object userIdObj = body.get("userId");
        if (userIdObj != null && !"null".equals(userIdObj.toString())) {
            try {
                userId = Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid userId")
                        .type(MediaType.TEXT_PLAIN)
                        .build();
            }
        }

        // Lấy amount: chấp nhận "amount" hoặc "total"
        Object amountObj = body.get("amount");
        if (amountObj == null) amountObj = body.get("total"); // fallback
        if (amountObj == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing required field: amount (or total)")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        Long amount;
        try {
            // amountObj có thể là Number hoặc String
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).longValue();
            } else {
                amount = Long.valueOf(amountObj.toString());
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid amount")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        CreateMomoResponse resp = paymentService.createPaymentAndRequestMomo(orderId, userId, amount, null);
        return Response.ok(resp).build();
    }

    @GET
    @Path("/status/{requestId}")
    public Response getStatus(@PathParam("requestId") String requestId) {
        PaymentStatusResponse resp = paymentService.checkMomoStatus(requestId);
        return Response.ok(resp).build();
    }

    @GET
    @Path("/check-status")
    public Response checkPaymentStatus(
            @QueryParam("orderId") String orderId,
            @QueryParam("resultCode") String resultCode
    ) {
        try {
            // Kiểm tra trạng thái thanh toán từ MoMo
            boolean isPaid = paymentService.checkPaymentStatus(orderId, resultCode);

            Map<String, String> response = new HashMap<>();
            if (isPaid) {
                response.put("status", "PAID");
            } else {
                response.put("status", "FAILED");
            }

            logger.info("PPPPPPPPPayment status for orderId " +
                    orderId + ": " + response.get("status"));

            return Response.ok(response).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("status", "ERROR"))
                    .build();
        }
    }
}