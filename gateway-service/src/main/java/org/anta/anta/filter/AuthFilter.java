package org.anta.anta.filter;


import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.anta.anta.config.JwtUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class AuthFilter implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(AuthFilter.class);

    @Inject
    JwtUtil jwtUtil;

    @ConfigProperty(
            name = "gateway.security.open-prefixes",
            defaultValue = "/api/auth,/api/user,/api/product,/api/products,/api/public,/api/cloud,/api/address,/api/orders,/api/cart,/api/payments,/api/momo,/api/categories,/api/dashboard,/api/staff,/actuator"
    )
    String openPrefixesConfig;

    @Override
    public void handle(RoutingContext context) {
        if (context.request().method() == HttpMethod.OPTIONS) {
            context.response().setStatusCode(200).end();
            return;
        }

        String path = context.request().path();
        String contentType = context.request().getHeader("Content-Type");

        LOG.infof("AuthFilter incoming path=%s Content-Type=%s", path, contentType);

        for (String prefix : getOpenPrefixes()) {
            if (path.startsWith(prefix)) {
                context.next();
                return;
            }
        }

        String authHeader = context.request().getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            context.response().setStatusCode(401).end();
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            context.response().setStatusCode(401).end();
            return;
        }

        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        /*
         * Tương đương:
         *
         * exchange.getRequest().mutate()
         *      .header("X-User-Name", ...)
         *      .header("X-User-Role", ...)
         *
         * Trong Vert.x, request headers là MultiMap nên có thể set trực tiếp.
         */
        context.request().headers().set("X-User-Name", username == null ? "" : username);
        context.request().headers().set("X-User-Role", role == null ? "" : role);

        context.next();
    }

    private List<String> getOpenPrefixes() {
        return Arrays.stream(openPrefixesConfig.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}