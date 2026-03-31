package cn.edu.tju.food.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

@Configuration
public class BusinessServiceFeignConfiguration {
  @Bean
  public RequestInterceptor businessServiceRequestInterceptor(
      @Value("${internal.service.token}") String internalServiceToken) {
    return requestTemplate -> {
      requestTemplate.header("X-Internal-Service-Token", internalServiceToken);
      requestTemplate.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    };
  }
}