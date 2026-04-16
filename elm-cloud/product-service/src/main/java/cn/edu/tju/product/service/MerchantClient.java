package cn.edu.tju.product.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.product.service.dto.BusinessDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class MerchantClient {

    private static final Logger log = LoggerFactory.getLogger(MerchantClient.class);

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    public MerchantClient(RestTemplate restTemplate, HttpServletRequest request) {
        this.restTemplate = restTemplate;
        this.request = request;
    }

    public Optional<BusinessDto> getBusinessById(Long businessId) {
        if (businessId == null) return Optional.empty();
        try {
            HttpHeaders headers = new HttpHeaders();
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                headers.set("Authorization", token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Use service discovery (Spring Cloud LoadBalancer) instead of assuming port 80
            String url = "lb://merchant-service/elm/api/businesses/" + businessId;

            try {
                ResponseEntity<HttpResult<BusinessDto>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<HttpResult<BusinessDto>>() {}
                );
                HttpResult<BusinessDto> body = response.getBody();
                if (body != null && Boolean.TRUE.equals(body.getSuccess()) && body.getData() != null) {
                    return Optional.of(body.getData());
                }
                return Optional.empty();
            } catch (Exception wrappedParseEx) {
                // fallback: sometimes it may directly return BusinessDto
                ResponseEntity<BusinessDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, BusinessDto.class);
                return Optional.ofNullable(response.getBody());
            }

        } catch (Exception e) {
            log.warn("getBusinessById({}) failed: {}", businessId, e.toString());
            return Optional.empty();
        }
    }

    public List<BusinessDto> getMyBusinesses() {
        HttpHeaders headers = new HttpHeaders();
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            headers.set("Authorization", token);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "lb://merchant-service/elm/api/businesses/my";
        ResponseEntity<HttpResult<List<BusinessDto>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<HttpResult<List<BusinessDto>>>() {}
        );
        HttpResult<List<BusinessDto>> body = response.getBody();
        if (body != null && Boolean.TRUE.equals(body.getSuccess()) && body.getData() != null) {
            return body.getData();
        }
        return List.of();
    }
}
