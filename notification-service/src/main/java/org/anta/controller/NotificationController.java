package org.anta.controller;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.anta.config.RabbitConfig;
import org.anta.dto.request.NotificationEmailRequest;
import org.anta.dto.request.OrderSuccessEmailRequest;
import org.anta.dto.response.NotificationResponse;
import org.anta.entity.NotificationRequestEntity;
import org.anta.repository.NotificationRequestRepository;
import org.anta.service.NotificationPublisherService;
import org.anta.util.JsonUtil;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationController {

    @Inject
    NotificationRequestRepository repo;

    @Inject
    NotificationPublisherService publisherService;

    @Inject
    @Channel("notifications-out")
    Emitter<String> emitter;

    @POST
    @Path("/email")
    @Transactional
    public Response sendEmail(@Valid NotificationEmailRequest req) {
        String requestId = UUID.randomUUID().toString();

        NotificationRequestEntity entity = NotificationRequestEntity.builder()
                .id(requestId)
                .type("EMAIL")
                .channel("EMAIL")
                .payload(JsonUtil.toJson(req))
                .status("PENDING")
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .idempotencyKey(req.getIdempotencyKey())
                .build();

        repo.save(entity);

        OutgoingRabbitMQMetadata metadata = OutgoingRabbitMQMetadata.builder()
                .withRoutingKey(RabbitConfig.EMAIL_ROUTING_KEY)
                .build();

        emitter.send(Message.of(requestId).addMetadata(metadata));

        return Response.accepted(new NotificationResponse(
                true,
                "Accepted",
                requestId
        )).build();
    }

    @POST
    @Path("/order-success")
    public Response sendOrderSuccess(@Valid OrderSuccessEmailRequest req) {

        Map<String, Object> data = new HashMap<>();
        data.put("customerName", req.getCustomerName() == null ? "bạn" : req.getCustomerName());
        data.put("orderNumber", req.getOrderNumber());

        if (req.getTotal() != null) {
            String vnd = NumberFormat.getInstance(new Locale("vi", "VN")).format(req.getTotal());
            data.put("total", vnd);
        }

        NotificationEmailRequest mail = new NotificationEmailRequest();
        mail.setTo(req.getTo());
        mail.setSubject("Xác nhận đơn hàng #" + req.getOrderNumber() + " - ANTA Việt Nam");
        mail.setTemplateId("order_success_v1");
        mail.setTemplateData(data);

        String key = (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank())
                ? req.getIdempotencyKey()
                : ("order_success:" + req.getOrderNumber() + ":" + req.getTo());

        mail.setIdempotencyKey(key);

        String requestId = publisherService.publishEmail(mail);

        return Response.accepted(new NotificationResponse(
                true,
                "Accepted",
                requestId
        )).build();
    }
}