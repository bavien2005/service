package org.anta.service;

import org.anta.dto.request.NotificationEmailRequest;

public interface NotificationPublisherService {

    String publishEmail(NotificationEmailRequest req);
}