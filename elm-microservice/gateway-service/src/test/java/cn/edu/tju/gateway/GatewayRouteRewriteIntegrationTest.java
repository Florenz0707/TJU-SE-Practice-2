package cn.edu.tju.gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.config.enabled=false",
      "spring.cloud.config.import-check.enabled=false",
      "eureka.client.enabled=false",
      "spring.cloud.discovery.enabled=false",
      "spring.cloud.gateway.server.webflux.discovery.locator.enabled=false"
    })
class GatewayRouteRewriteIntegrationTest {

  private static HttpServer backend;
  private static int backendPort;

  @LocalServerPort private int gatewayPort;

  @Autowired private WebTestClient webTestClient;

  @MockBean private ReactiveDiscoveryClient reactiveDiscoveryClient;

  @BeforeAll
  static void startBackend() throws IOException {
    backend = HttpServer.create(new InetSocketAddress(0), 0);
    backendPort = backend.getAddress().getPort();
    backend.createContext("/elm/api/ping", GatewayRouteRewriteIntegrationTest::writePathResponse);
    backend.createContext("/elm/healthz", GatewayRouteRewriteIntegrationTest::writePathResponse);
    backend.createContext("/elm/check", GatewayRouteRewriteIntegrationTest::writePathResponse);
    backend.start();
  }

  @AfterAll
  static void stopBackend() {
    if (backend != null) {
      backend.stop(0);
    }
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("ELM_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("POINTS_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("ACCOUNT_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("BUSINESS_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("FOOD_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("CART_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("ORDER_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("ADDRESS_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
    registry.add("USER_SERVICE_URL", () -> "http://127.0.0.1:" + backendPort);
  }

  @Test
  void rewritesApiRequestsToElmApiPrefix() {
    assertNotNull(webTestClient);

    webTestClient
        .get()
        .uri("http://127.0.0.1:" + gatewayPort + "/api/ping")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.path")
        .isEqualTo("/elm/api/ping");
  }

  @Test
  void passesElmContextRequestsThroughWithoutRewrite() {
    webTestClient
        .get()
        .uri("http://127.0.0.1:" + gatewayPort + "/elm/healthz")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.path")
        .isEqualTo("/elm/healthz");
  }

        @Test
        void rewritesDirectServiceRequestsToElmPrefix() {
          webTestClient
          .get()
          .uri("http://127.0.0.1:" + gatewayPort + "/services/address/check")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.path")
          .isEqualTo("/elm/check");
        }

  private static void writePathResponse(HttpExchange exchange) throws IOException {
    byte[] body = ("{\"path\":\"" + exchange.getRequestURI().getPath() + "\"}").getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(body);
    }
  }
}