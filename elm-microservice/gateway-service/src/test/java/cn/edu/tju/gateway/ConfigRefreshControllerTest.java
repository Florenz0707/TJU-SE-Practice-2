package cn.edu.tju.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ConfigRefreshControllerTest {

  @Mock private ReactiveDiscoveryClient discoveryClient;

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
}