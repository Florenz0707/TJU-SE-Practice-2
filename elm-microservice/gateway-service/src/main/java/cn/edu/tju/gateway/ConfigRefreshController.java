package cn.edu.tju.gateway;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/config")
public class ConfigRefreshController {
  private static final Set<String> EXCLUDED_SERVICES = Set.of("config-server", "discovery-server");

  private final ReactiveDiscoveryClient discoveryClient;
  private final WebClient webClient;
  private final String refreshToken;

  public ConfigRefreshController(
      ReactiveDiscoveryClient discoveryClient,
      WebClient.Builder webClientBuilder,
      @Value("${config.refresh.token}") String refreshToken) {
    this.discoveryClient = discoveryClient;
    this.webClient = webClientBuilder.build();
    this.refreshToken = refreshToken;
  }

  @PostMapping("/refresh")
  public Mono<ResponseEntity<Map<String, Object>>> refreshAll(
      @RequestHeader(value = "X-Config-Refresh-Token", required = false) String requestToken) {
    if (!refreshToken.equals(requestToken)) {
      return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
          "success", false,
          "message", "invalid refresh token")));
    }

    return discoveryClient.getServices()
        .filter(serviceId -> !EXCLUDED_SERVICES.contains(serviceId))
        .flatMap(this::refreshServiceInstances)
        .collectList()
        .map(results -> {
          long successCount = results.stream().filter(result -> Boolean.TRUE.equals(result.get("success"))).count();
          return ResponseEntity.ok(Map.of(
              "success", true,
              "targetCount", results.size(),
              "successCount", successCount,
              "results", results));
      })
      .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
        "success", false,
        "message", "service discovery unavailable",
        "error", error.getClass().getSimpleName() + ": " + error.getMessage()))));
  }

  private Flux<Map<String, Object>> refreshServiceInstances(String serviceId) {
    return discoveryClient.getInstances(serviceId)
      .flatMap(instance -> refreshInstance(serviceId, instance))
      .onErrorResume(error -> Flux.just(buildResult(
        serviceId,
        null,
        "discovery://" + serviceId + "/instances",
        false,
        0,
        error.getClass().getSimpleName() + ": " + error.getMessage())));
  }

  private Mono<Map<String, Object>> refreshInstance(String serviceId, ServiceInstance instance) {
    URI refreshEndpoint = instance.getUri().resolve("/actuator/refresh");
    return webClient.post()
        .uri(refreshEndpoint)
        .retrieve()
        .bodyToMono(List.class)
        .map(changedKeys -> buildResult(serviceId, instance.getInstanceId(), refreshEndpoint.toString(), true, changedKeys.size(), null))
        .onErrorResume(error -> Mono.just(buildResult(
            serviceId,
            instance.getInstanceId(),
            refreshEndpoint.toString(),
            false,
            0,
            error.getClass().getSimpleName() + ": " + error.getMessage())));
  }

  private Map<String, Object> buildResult(
      String serviceId,
      String instanceId,
      String refreshEndpoint,
      boolean success,
      int changedKeyCount,
      String errorMessage) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("serviceId", serviceId);
    result.put("instanceId", instanceId);
    result.put("endpoint", refreshEndpoint);
    result.put("success", success);
    result.put("changedKeyCount", changedKeyCount);
    if (errorMessage != null) {
      result.put("error", errorMessage);
    }
    return result;
  }
}