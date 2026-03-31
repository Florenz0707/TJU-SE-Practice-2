package cn.edu.tju.core.security.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AccountProvisioningClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalServiceToken;

  public AccountProvisioningClient(
      RestTemplate restTemplate,
      @Value("${account.service.url:http://localhost:8082/elm}") String accountServiceUrl,
      @Value("${internal.service.token}") String internalServiceToken) {
    this.restTemplate = restTemplate;
    this.baseUrl =
        accountServiceUrl.endsWith("/")
            ? accountServiceUrl.substring(0, accountServiceUrl.length() - 1)
            : accountServiceUrl;
    this.internalServiceToken = internalServiceToken;
  }

  public void ensureWallet(Long userId) {
    String url = baseUrl + "/api/inner/account/wallet/by-user/" + userId + "?createIfAbsent=true";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Service-Token", internalServiceToken);
    HttpEntity<Void> request = new HttpEntity<>(headers);
    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
    Map<?, ?> body = response.getBody();
    if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
      throw new IllegalStateException("wallet provisioning failed");
    }
  }
}