package cn.edu.tju.food.client;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BusinessLookupClient {
  private final BusinessServiceFeignClient businessServiceFeignClient;

  public BusinessLookupClient(BusinessServiceFeignClient businessServiceFeignClient) {
    this.businessServiceFeignClient = businessServiceFeignClient;
  }

  public boolean exists(Long businessId) {
    if (businessId == null) {
      return false;
    }
    try {
      Map<?, ?> body = businessServiceFeignClient.getBusinessById(businessId);
      return body != null && Boolean.TRUE.equals(body.get("success")) && body.get("data") instanceof Map;
    } catch (Exception e) {
      return false;
    }
  }
}
