package org.anta.anta.config;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.anta.anta.filter.AuthFilter;
import org.anta.anta.filter.CorsResponseNormalizationFilter;
import org.anta.anta.filter.LoggingFilter;
import org.anta.anta.proxy.CloudProxyHandler;

@ApplicationScoped
public class GatewayRouter {

    @Inject
    CorsResponseNormalizationFilter corsResponseNormalizationFilter;

    @Inject
    AuthFilter authFilter;

    @Inject
    LoggingFilter loggingFilter;

    @Inject
    CloudProxyHandler cloudProxyHandler;

    public void registerRoutes(@Observes Router router) {
        // Quan trọng: phải có BodyHandler trước khi đọc body trong proxy
        router.route().order(-120).handler(
                BodyHandler.create()
                        .setHandleFileUploads(true)
                        .setBodyLimit(200L * 1024L * 1024L)
        );

        router.route().order(-110).handler(corsResponseNormalizationFilter);
        router.route().order(-100).handler(authFilter);
        router.route().order(-90).handler(loggingFilter);
        router.route().order(0).handler(cloudProxyHandler);
    }
}