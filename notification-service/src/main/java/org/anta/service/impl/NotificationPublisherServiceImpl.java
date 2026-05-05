package org.anta.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.config.RabbitConfig;
import org.anta.dto.request.NotificationEmailRequest;
import org.anta.entity.NotificationRequestEntity;
import org.anta.repository.NotificationRequestRepository;
import org.anta.service.NotificationPublisherService;
import org.anta.util.JsonUtil;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class NotificationPublisherServiceImpl implements NotificationPublisherService {

    @Inject
    NotificationRequestRepository repo;

    @Inject
    @Channel("notifications-out")
    Emitter<String> emitter;

    @Override
    @Transactional
    public String publishEmail(NotificationEmailRequest req) {

        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            var existed = repo.findByIdempotencyKey(req.getIdempotencyKey());
            if (existed.isPresent()) {
                return existed.get().getId();
            }
        }

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

        return requestId;
    }
}