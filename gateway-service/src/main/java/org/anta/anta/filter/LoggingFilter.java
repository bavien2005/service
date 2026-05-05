package org.anta.anta.filter;


import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LoggingFilter implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class);

    @Override
    public void handle(RoutingContext context) {
        String path = context.request().path();
        String method = context.request().method() != null
                ? context.request().method().name()
                : "UNKNOWN";

        String user = context.request().getHeader("X-User-Name");
        String role = context.request().getHeader("X-User-Role");

        LOG.infof("[GATEWAY] %s %s (user=%s, role=%s)", method, path, user, role);

        context.next();
    }
}