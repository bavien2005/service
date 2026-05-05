package org.anta.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.dto.request.NotificationEmailRequest;
import org.anta.entity.NotificationRequestEntity;
import org.anta.repository.NotificationRequestRepository;
import org.anta.service.TemplateRenderer;
import org.anta.util.JsonUtil;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class EmailWorker {

    private static final Logger LOG = Logger.getLogger(EmailWorker.class);

    @Inject
    NotificationRequestRepository repo;

    @Inject
    Mailer mailer;

    @Inject
    TemplateRenderer templateRenderer;

    @Incoming("notifications-in")
    @Transactional
    public void handle(String requestId) {
        Optional<NotificationRequestEntity> opt = repo.findByIdOptional(requestId);

        if (opt.isEmpty()) {
            LOG.warnf("[EMAIL WORKER] Request not found: %s", requestId);
            return;
        }

        NotificationRequestEntity entity = opt.get();

        if ("SENT".equals(entity.getStatus())) {
            return;
        }

        try {
            entity.setStatus("PROCESSING");
            repo.save(entity);

            NotificationEmailRequest req = JsonUtil.fromJson(
                    entity.getPayload(),
                    new TypeReference<NotificationEmailRequest>() {
                    }
            );

            String html;

            if (req.getRawHtml() != null && !req.getRawHtml().isBlank()) {
                html = req.getRawHtml();
            } else if (req.getBody() != null && !req.getBody().isBlank()) {
                html = req.getBody().replace("\n", "<br>");
            } else {
                html = templateRenderer.render(req.getTemplateId(), req.getTemplateData());
            }

            sendHtmlEmail(req.getTo(), req.getSubject(), html);

            entity.setStatus("SENT");
            entity.setAttempts(entity.getAttempts() == null ? 1 : entity.getAttempts() + 1);
            entity.setUpdatedAt(LocalDateTime.now());
            repo.save(entity);

            LOG.infof("[EMAIL WORKER] Email sent. requestId=%s to=%s", requestId, req.getTo());

        } catch (Exception ex) {
            int attempts = entity.getAttempts() == null ? 1 : entity.getAttempts() + 1;

            entity.setAttempts(attempts);
            entity.setStatus(attempts >= 3 ? "FAILED" : "PENDING");
            entity.setUpdatedAt(LocalDateTime.now());
            repo.save(entity);

            LOG.errorf(ex, "[EMAIL WORKER] Failed to send email. requestId=%s attempts=%d", requestId, attempts);
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        mailer.send(Mail.withHtml(to, subject, html));
    }
}