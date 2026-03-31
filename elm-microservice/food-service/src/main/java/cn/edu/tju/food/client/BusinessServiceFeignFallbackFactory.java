package cn.edu.tju.food.client;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.cloud.openfeign.FallbackFactory;

@Component
public class BusinessServiceFeignFallbackFactory implements FallbackFactory<BusinessServiceFeignClient> {
  @Override
  public BusinessServiceFeignClient create(Throwable cause) {
    return businessId ->
        Map.of(
            "success", false,
            "message", "business-service unavailable, degraded by circuit breaker",
            "businessId", businessId,
            "cause", cause == null ? "unknown" : cause.getClass().getSimpleName());
  }
}