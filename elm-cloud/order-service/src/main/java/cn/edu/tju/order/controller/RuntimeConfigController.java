package cn.edu.tju.order.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.order.config.RefreshableDemoProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class RuntimeConfigController {
  private final RefreshableDemoProperties refreshableDemoProperties;

  public RuntimeConfigController(RefreshableDemoProperties refreshableDemoProperties) {
    this.refreshableDemoProperties = refreshableDemoProperties;
  }

  @GetMapping("/api/orders/runtime-config")
  public Map<String, String> getRuntimeConfig() {
    return Map.of(
        "service", "order-service",
        "message", refreshableDemoProperties.getMessage(),
        "version", refreshableDemoProperties.getVersion());
  }
}