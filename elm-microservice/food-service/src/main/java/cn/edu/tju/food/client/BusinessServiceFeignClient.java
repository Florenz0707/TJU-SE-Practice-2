package cn.edu.tju.food.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "business-service",
    path = "/elm/api/inner/business",
    configuration = BusinessServiceFeignConfiguration.class,
    fallbackFactory = BusinessServiceFeignFallbackFactory.class)
public interface BusinessServiceFeignClient {
  @GetMapping("/{businessId}")
  Map<String, Object> getBusinessById(@PathVariable("businessId") Long businessId);
}