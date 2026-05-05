package org.anta.anta.proxy;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.anta.anta.filter.DebugForwardLoggingFilter;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class CloudProxyHandler implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(CloudProxyHandler.class);

    private static final Set<String> SKIP_REQUEST_HEADERS = Set.of(
            "host",
            "content-length",
            "connection",
            "accept-encoding",
            "origin",
            "access-control-request-method",
            "access-control-request-headers"
    );

    private static final Set<String> SKIP_RESPONSE_HEADERS = Set.of(
            "content-length",
            "transfer-encoding",
            "connection"
    );

    @Inject
    Vertx vertx;

    @Inject
    DebugForwardLoggingFilter debugForwardLoggingFilter;

    private WebClient webClient;

    private List<RouteItem> routes;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
        this.routes = loadRoutes();

        LOG.infof("[GATEWAY] Loaded routes: %s", routes);
    }

    @Override
    public void handle(RoutingContext context) {
        String path = context.request().path();

        Optional<RouteItem> matchedRoute = findRoute(path);

        if (matchedRoute.isEmpty()) {
            context.response().setStatusCode(404).end();
            return;
        }

        RouteItem route = matchedRoute.get();

        Buffer body = context.body() != null ? context.body().buffer() : null;
        forward(context, route, body);
    }

    private List<RouteItem> loadRoutes() {
        List<RouteItem> result = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            String idKey = "gateway.routes[" + i + "].id";
            String prefixKey = "gateway.routes[" + i + "].prefix";
            String targetKey = "gateway.routes[" + i + "].target";

            Optional<String> id = ConfigProvider.getConfig().getOptionalValue(idKey, String.class);
            Optional<String> prefix = ConfigProvider.getConfig().getOptionalValue(prefixKey, String.class);
            Optional<String> target = ConfigProvider.getConfig().getOptionalValue(targetKey, String.class);

            if (id.isEmpty() || prefix.isEmpty() || target.isEmpty()) {
                continue;
            }

            result.add(new RouteItem(id.get(), prefix.get(), target.get()));
        }

        return result;
    }

    private Optional<RouteItem> findRoute(String path) {
        return routes.stream()
                .filter(route -> path.startsWith(route.prefix()))
                .max(Comparator.comparingInt(route -> route.prefix().length()));
    }

    private void forward(RoutingContext context, RouteItem route, Buffer body) {
        String targetUrl = buildTargetUrl(context, route);

        debugForwardLoggingFilter.logForward(context.request().path(), targetUrl);

        HttpRequest<Buffer> targetRequest = webClient
                .requestAbs(context.request().method(), targetUrl)
                .timeout(30000);

        copyRequestHeaders(context, targetRequest);
        addForwardedHeaders(context, targetRequest);

        if (body != null && body.length() > 0) {
            targetRequest.sendBuffer(body)
                    .onSuccess(response -> copyResponse(context, response))
                    .onFailure(error -> handleForwardError(context, route, error));
        } else {
            targetRequest.send()
                    .onSuccess(response -> copyResponse(context, response))
                    .onFailure(error -> handleForwardError(context, route, error));
        }
    }

    private String buildTargetUrl(RoutingContext context, RouteItem route) {
        String baseUrl = route.target();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String path = context.request().path();
        String query = context.request().query();

        if (query == null || query.isBlank()) {
            return baseUrl + path;
        }

        return baseUrl + path + "?" + query;
    }

    private void copyRequestHeaders(RoutingContext context, HttpRequest<Buffer> targetRequest) {
        context.request().headers().forEach(header -> {
            String name = header.getKey();
            String value = header.getValue();

            if (name == null) {
                return;
            }

            String lowerName = name.toLowerCase();

            if (!SKIP_REQUEST_HEADERS.contains(lowerName)) {
                targetRequest.putHeader(name, value);
            }
        });
    }

    private void addForwardedHeaders(RoutingContext context, HttpRequest<Buffer> targetRequest) {
        String host = context.request().host();

        if (host != null) {
            targetRequest.putHeader("X-Forwarded-Host", host);
        }

        if (context.request().remoteAddress() != null) {
            targetRequest.putHeader("X-Forwarded-For", context.request().remoteAddress().host());
        }

        targetRequest.putHeader("X-Forwarded-Proto", context.request().scheme());
    }

    private void copyResponse(RoutingContext context, HttpResponse<Buffer> response) {
        context.response().setStatusCode(response.statusCode());

        response.headers().forEach(header -> {
            String name = header.getKey();
            String value = header.getValue();

            if (name == null) {
                return;
            }

            String lowerName = name.toLowerCase();

            if (!SKIP_RESPONSE_HEADERS.contains(lowerName)) {
                context.response().putHeader(name, value);
            }
        });

        Buffer responseBody = response.body();

        if (responseBody == null) {
            context.response().end();
        } else {
            context.response().end(responseBody);
        }
    }

    private void handleForwardError(RoutingContext context, RouteItem route, Throwable error) {
        LOG.errorf(error, "Cannot forward request to route=%s target=%s", route.id(), route.target());
        context.response().setStatusCode(502).end();
    }

    private record RouteItem(String id, String prefix, String target) {
    }
}