package cn.edu.tju.gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.config.enabled=false",
      "spring.cloud.config.import-check.enabled=false",
      "config.refresh.token=test-config-refresh-token",
      "eureka.client.enabled=false",
      "spring.cloud.gateway.server.webflux.discovery.locator.enabled=false"
    })
class GatewayRouteRewriteIntegrationTest {

  private static HttpServer backend;
  private static int backendPort;

  @LocalServerPort private int gatewayPort;

  @Autowired private WebTestClient webTestClient;

  @MockitoBean private ReactiveDiscoveryClient reactiveDiscoveryClient;

    @BeforeEach
    void mockLoadBalancedInstances() {
    when(reactiveDiscoveryClient.getInstances("elm"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "elm-1", "elm", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("points-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "points-1", "points-service", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("account-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "account-1", "account-service", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("business-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "business-1", "business-service", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("food-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "food-1", "food-service", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("cart-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "cart-1", "cart-service", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("order-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "order-1", "order-service", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("address-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "address-1", "address-service", "127.0.0.1", backendPort, false)));
    when(reactiveDiscoveryClient.getInstances("user-service"))
      .thenReturn(
        Flux.just(
          new DefaultServiceInstance(
            "user-1", "user-service", "127.0.0.1", backendPort, false)));
    }

  @BeforeAll
  static void startBackend() throws IOException {
    backend = HttpServer.create(new InetSocketAddress(0), 0);
    backendPort = backend.getAddress().getPort();
    backend.createContext("/elm/api/ping", GatewayRouteRewriteIntegrationTest::writePathResponse);
    backend.createContext("/elm/healthz", GatewayRouteRewriteIntegrationTest::writePathResponse);
    backend.createContext("/elm/check", GatewayRouteRewriteIntegrationTest::writePathResponse);
    backend.createContext("/elm/v3/api-docs", GatewayRouteRewriteIntegrationTest::writePathResponse);
    backend.createContext("/elm/profile", GatewayRouteRewriteIntegrationTest::writePathResponse);
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
    registry.add("ELM_SERVICE_URL", () -> "lb://elm");
    registry.add("POINTS_SERVICE_URL", () -> "lb://points-service");
    registry.add("ACCOUNT_SERVICE_URL", () -> "lb://account-service");
    registry.add("BUSINESS_SERVICE_URL", () -> "lb://business-service");
    registry.add("FOOD_SERVICE_URL", () -> "lb://food-service");
    registry.add("CART_SERVICE_URL", () -> "lb://cart-service");
    registry.add("ORDER_SERVICE_URL", () -> "lb://order-service");
    registry.add("ADDRESS_SERVICE_URL", () -> "lb://address-service");
    registry.add("USER_SERVICE_URL", () -> "lb://user-service");
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

        @Test
        void rewritesOpenApiRequestsToElmPrefix() {
          webTestClient
          .get()
          .uri("http://127.0.0.1:" + gatewayPort + "/v3/api-docs")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.path")
          .isEqualTo("/elm/v3/api-docs");
        }

        @Test
        void rewritesUserServiceDirectRequestsToElmPrefix() {
          webTestClient
          .get()
          .uri("http://127.0.0.1:" + gatewayPort + "/services/user/profile")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.path")
          .isEqualTo("/elm/profile");
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