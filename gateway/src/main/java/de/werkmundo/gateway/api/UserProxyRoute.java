package de.werkmundo.gateway.api;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UserProxyRoute {

    private static final String PREFIX = "/svc/user";

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "gateway.user.host")
    String host;

    @ConfigProperty(name = "gateway.user.port")
    int port;

    private HttpClient client;
    private HttpProxy proxy;

    void initialize(@Observes Router router) {
        client = vertx.createHttpClient(
                new HttpClientOptions().setConnectTimeout(5000));

        proxy = HttpProxy.reverseProxy(client).origin(port, host);

        proxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(
                    ProxyContext context) {

                String originalUri = context.request().getURI();
                context.request().setURI(
                        originalUri.substring(PREFIX.length()));

                return context.sendRequest();
            }
        });
        router.route("/svc/user/*").handler(this::forward);
    }


    public void forward(RoutingContext context) {
        proxy.handle(context.request());
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }
}