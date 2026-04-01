package cn.edu.tju.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ConfigRefreshControllerTest {

  private static HttpServer backend;
  private static int backendPort;

  @Mock private ReactiveDiscoveryClient discoveryClient;

  @BeforeAll
  static void startBackend() throws IOException {
    backend = HttpServer.create(new InetSocketAddress(0), 0);
    backendPort = backend.getAddress().getPort();
    backend.createContext("/actuator/refresh", ConfigRefreshControllerTest::writeRefreshResponse);
    backend.start();
  }

  @AfterAll
  static void stopBackend() {
    if (backend != null) {
      backend.stop(0);
    }
  }

  @Test
  void refreshAllRejectsInvalidToken() {
    ConfigRefreshController controller =
        new ConfigRefreshController(discoveryClient, WebClient.builder(), "expected-token");

    ResponseEntity<Map<String, Object>> response = controller.refreshAll("wrong-token").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).containsEntry("success", false);
    verify(discoveryClient, never()).getServices();
  }

  @Test
  void refreshAllSkipsExcludedServicesAndAggregatesEmptyResults() {
    when(discoveryClient.getServices()).thenReturn(Flux.just("config-server", "order-service"));
    when(discoveryClient.getInstances("order-service")).thenReturn(Flux.empty());

    ConfigRefreshController controller =
        new ConfigRefreshController(discoveryClient, WebClient.builder(), "expected-token");

    ResponseEntity<Map<String, Object>> response = controller.refreshAll("expected-token").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .containsEntry("success", true)
        .containsEntry("targetCount", 0)
        .containsEntry("successCount", 0L);
    verify(discoveryClient, never()).getInstances("config-server");
    verify(discoveryClient).getInstances("order-service");
  }

  @Test
  void refreshAllAggregatesSuccessfulAndFailedInstanceResults() {
    ServiceInstance successInstance =
        new DefaultServiceInstance(
            "order-service-1",
            "order-service",
            "localhost",
            backendPort,
            false);
    ServiceInstance failedInstance =
        new DefaultServiceInstance(
            "wallet-service-1",
            "wallet-service",
            "127.0.0.1",
            1,
            false);

    when(discoveryClient.getServices()).thenReturn(Flux.just("order-service", "wallet-service"));
    when(discoveryClient.getInstances("order-service")).thenReturn(Flux.just(successInstance));
    when(discoveryClient.getInstances("wallet-service")).thenReturn(Flux.just(failedInstance));

    ConfigRefreshController controller =
        new ConfigRefreshController(discoveryClient, WebClient.builder(), "expected-token");

    ResponseEntity<Map<String, Object>> response = controller.refreshAll("expected-token").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .containsEntry("success", true)
        .containsEntry("targetCount", 2)
        .containsEntry("successCount", 1L);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
    assertThat(results).hasSize(2);
    Map<String, Map<String, Object>> resultsByServiceId =
      results.stream()
        .collect(Collectors.toMap(result -> (String) result.get("serviceId"), Function.identity()));
    Map<String, Object> success = resultsByServiceId.get("order-service");
    Map<String, Object> failed = resultsByServiceId.get("wallet-service");
    assertThat(success).isNotNull();
    assertThat(failed).isNotNull();
    assertThat(success)
        .containsEntry("serviceId", "order-service")
        .containsEntry("instanceId", "order-service-1")
        .containsEntry("success", true)
        .containsEntry("changedKeyCount", 2);
    assertThat(((String) success.get("endpoint"))).isEqualTo("http://localhost:" + backendPort + "/actuator/refresh");
    assertThat(failed)
        .containsEntry("serviceId", "wallet-service")
        .containsEntry("instanceId", "wallet-service-1")
        .containsEntry("success", false)
        .containsKey("error");
  }

    @Test
    void refreshAllReturnsStructuredServiceUnavailableWhenDiscoveryFails() {
    when(discoveryClient.getServices()).thenReturn(Flux.error(new IllegalStateException("eureka offline")));

    ConfigRefreshController controller =
      new ConfigRefreshController(discoveryClient, WebClient.builder(), "expected-token");

    ResponseEntity<Map<String, Object>> response = controller.refreshAll("expected-token").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody())
      .containsEntry("success", false)
      .containsEntry("message", "service discovery unavailable");
    assertThat((String) response.getBody().get("error")).contains("IllegalStateException");
    }

    @Test
    void refreshAllReportsInstanceDiscoveryFailureAsFailedResult() {
    ServiceInstance successInstance =
      new DefaultServiceInstance("order-service-1", "order-service", "localhost", backendPort, false);

    when(discoveryClient.getServices()).thenReturn(Flux.just("order-service", "wallet-service"));
    when(discoveryClient.getInstances("order-service")).thenReturn(Flux.just(successInstance));
    when(discoveryClient.getInstances("wallet-service"))
      .thenReturn(Flux.error(new IllegalStateException("wallet registry unavailable")));

    ConfigRefreshController controller =
      new ConfigRefreshController(discoveryClient, WebClient.builder(), "expected-token");

    ResponseEntity<Map<String, Object>> response = controller.refreshAll("expected-token").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
      .containsEntry("success", true)
      .containsEntry("targetCount", 2)
      .containsEntry("successCount", 1L);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
    Map<String, Map<String, Object>> resultsByServiceId =
      results.stream()
        .collect(Collectors.toMap(result -> (String) result.get("serviceId"), Function.identity()));

    assertThat(resultsByServiceId.get("wallet-service"))
      .containsEntry("serviceId", "wallet-service")
      .containsEntry("instanceId", null)
      .containsEntry("endpoint", "discovery://wallet-service/instances")
      .containsEntry("success", false)
      .containsKey("error");
    }

  private static void writeRefreshResponse(HttpExchange exchange) throws IOException {
    byte[] body = "[\"gateway.route\",\"db.password\"]".getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(body);
    }
  }
}