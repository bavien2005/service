package org.anta.anta.filter;


import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CorsResponseNormalizationFilter implements Handler<RoutingContext> {

    @ConfigProperty(name = "gateway.cors.allowed-origin", defaultValue = "http://localhost:5173")
    String allowedOrigin;

    @ConfigProperty(name = "gateway.cors.allowed-methods", defaultValue = "GET,POST,PUT,DELETE,OPTIONS,PATCH")
    String allowedMethods;

    @ConfigProperty(name = "gateway.cors.allowed-headers", defaultValue = "*")
    String allowedHeaders;

    @ConfigProperty(name = "gateway.cors.exposed-headers", defaultValue = "Authorization,X-User-Name,X-User-Role,Location")
    String exposedHeaders;

    @ConfigProperty(name = "gateway.cors.allow-credentials", defaultValue = "true")
    String allowCredentials;

    @ConfigProperty(name = "gateway.cors.max-age", defaultValue = "3600")
    String maxAge;

    @Override
    public void handle(RoutingContext context) {
        /*
         * Tương đương CorsResponseNormalizationFilter cũ:
         * - Xóa Access-Control-Allow-Origin cũ để tránh duplicate
         * - Add lại đúng 1 origin: http://localhost:5173
         *
         * Ở Vert.x dùng addHeadersEndHandler để chỉnh header ngay trước khi response gửi về client.
         */
        context.addHeadersEndHandler(event -> applyCorsHeaders(context.response()));

        if (context.request().method() == HttpMethod.OPTIONS) {
            context.response().setStatusCode(200).end();
            return;
        }

        context.next();
    }

    private void applyCorsHeaders(HttpServerResponse response) {
        response.headers().remove("Access-Control-Allow-Origin");
        response.headers().remove("Access-Control-Allow-Credentials");
        response.headers().remove("Access-Control-Allow-Methods");
        response.headers().remove("Access-Control-Allow-Headers");
        response.headers().remove("Access-Control-Expose-Headers");
        response.headers().remove("Access-Control-Max-Age");

        response.putHeader("Access-Control-Allow-Origin", allowedOrigin);
        response.putHeader("Access-Control-Allow-Credentials", allowCredentials);
        response.putHeader("Access-Control-Allow-Methods", allowedMethods);
        response.putHeader("Access-Control-Allow-Headers", allowedHeaders);
        response.putHeader("Access-Control-Expose-Headers", exposedHeaders);
        response.putHeader("Access-Control-Max-Age", maxAge);
    }
}