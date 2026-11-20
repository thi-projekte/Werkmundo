package de.werkmundo.gateway;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendTestResource implements QuarkusTestResourceLifecycleManager {

    private final List<HttpServer> servers = new ArrayList<>();

    @Override
    public Map<String, String> start() {
        Map<String, String> config = new HashMap<>();

        config.put("quarkus.oidc.tenant-enabled", "false");
        config.put("quarkus.oidc.auth-server-url",
                "http://127.0.0.1:1/realms/test");
        config.put("quarkus.http.test-port", "0");

        try {
            for (String service : List.of("user", "offer", "invoice", "catalog")) {
                HttpServer server = HttpServer.create(
                        new InetSocketAddress("127.0.0.1", 0), 0);

                server.createContext("/", exchange -> {
                    try {
                        String path = exchange.getRequestURI().getRawPath();
                        String body = new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8);

                       ByteArrayOutputStream output = new ByteArrayOutputStream();

                        try (JsonGenerator json = new JsonFactory().createGenerator(output)) {
                            json.writeStartObject();
                            json.writeStringField("service", service);
                            json.writeStringField("path", path);
                            json.writeStringField("query", exchange.getRequestURI().getRawQuery());
                            json.writeStringField("method", exchange.getRequestMethod());
                            json.writeStringField("body", body);
                            json.writeStringField("offerToken",
                                    exchange.getRequestHeaders().getFirst("X-Offer-Access-Token"));
                            json.writeEndObject();
                        }

                        byte[] response = output.toByteArray();

                        exchange.getResponseHeaders().set(
                                "Content-Type", "application/json");

                        int status = path.endsWith("/missing") ? 404 : 200;
                        exchange.sendResponseHeaders(status, response.length);
                        exchange.getResponseBody().write(response);
                    } catch (Throwable error) {
                        System.err.println("Fehler im Testbackend: " + service);
                        error.printStackTrace();
                    } finally {
                        exchange.close();
                    }
                });

                server.start();
                servers.add(server);

                config.put("gateway." + service + ".host", "127.0.0.1");
                config.put("gateway." + service + ".port",
                        Integer.toString(server.getAddress().getPort()));
            }
        } catch (IOException e) {
            stop();
            throw new IllegalStateException("Testserver konnte nicht starten", e);
        }

        return config;
    }

    @Override
    public void stop() {
        servers.forEach(server -> server.stop(0));
        servers.clear();
    }
}