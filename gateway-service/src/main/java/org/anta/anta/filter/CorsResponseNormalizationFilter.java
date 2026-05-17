package org.anta.anta.filter;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class CorsResponseNormalizationFilter implements Handler<RoutingContext> {

    @ConfigProperty(
            name = "gateway.cors.allowed-origins",
            defaultValue = "http://localhost:5173,http://localhost:5174"
    )
    String allowedOrigins;

    @ConfigProperty(name = "gateway.cors.allowed-methods", defaultValue = "GET,POST,PUT,DELETE,OPTIONS,PATCH")
    String allowedMethods;

    @ConfigProperty(name = "gateway.cors.allowed-headers", defaultValue = "Content-Type,Authorization,Accept,Origin,X-Requested-With")
    String allowedHeaders;

    @ConfigProperty(name = "gateway.cors.exposed-headers", defaultValue = "Authorization,X-User-Name,X-User-Role,Location")
    String exposedHeaders;

    @ConfigProperty(name = "gateway.cors.allow-credentials", defaultValue = "true")
    String allowCredentials;

    @ConfigProperty(name = "gateway.cors.max-age", defaultValue = "3600")
    String maxAge;

    @Override
    public void handle(RoutingContext context) {
        context.addHeadersEndHandler(event -> applyCorsHeaders(context));

        if (context.request().method() == HttpMethod.OPTIONS) {
            applyCorsHeaders(context);
            context.response().setStatusCode(200).end();
            return;
        }

        context.next();
    }

    private void applyCorsHeaders(RoutingContext context) {
        HttpServerResponse response = context.response();

        response.headers().remove("Access-Control-Allow-Origin");
        response.headers().remove("Access-Control-Allow-Credentials");
        response.headers().remove("Access-Control-Allow-Methods");
        response.headers().remove("Access-Control-Allow-Headers");
        response.headers().remove("Access-Control-Expose-Headers");
        response.headers().remove("Access-Control-Max-Age");

        String requestOrigin = context.request().getHeader("Origin");
        String originToReturn = resolveAllowedOrigin(requestOrigin);

        if (originToReturn != null) {
            response.putHeader("Access-Control-Allow-Origin", originToReturn);
        }

        response.putHeader("Access-Control-Allow-Credentials", allowCredentials);
        response.putHeader("Access-Control-Allow-Methods", allowedMethods);
        response.putHeader("Access-Control-Allow-Headers", allowedHeaders);
        response.putHeader("Access-Control-Expose-Headers", exposedHeaders);
        response.putHeader("Access-Control-Max-Age", maxAge);
    }

    private String resolveAllowedOrigin(String requestOrigin) {
        if (requestOrigin == null || requestOrigin.isBlank()) {
            return getAllowedOriginList().get(0);
        }

        return getAllowedOriginList().stream()
                .filter(origin -> origin.equalsIgnoreCase(requestOrigin))
                .findFirst()
                .orElse(null);
    }

    private List<String> getAllowedOriginList() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}