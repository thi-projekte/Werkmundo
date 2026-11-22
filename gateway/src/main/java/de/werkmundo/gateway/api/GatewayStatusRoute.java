package de.werkmundo.gateway.api;

import io.quarkus.security.Authenticated;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GatewayStatusRoute {

    @Authenticated
    @Route(path = "/gateway/status", methods = Route.HttpMethod.GET)
    public void status(RoutingContext context) {
        context.response()
                .putHeader("Content-Type", "application/json")
                .end("{\"status\":\"authenticated\"}");
    }
}