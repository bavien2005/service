package org.anta.config;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RabbitConfig {

    public static final String NOTIFICATIONS_EXCHANGE = "notifications-exchange";

    public static final String EMAIL_QUEUE = "notifications.email.queue";

    public static final String EMAIL_ROUTING_KEY = "notifications.email";
}