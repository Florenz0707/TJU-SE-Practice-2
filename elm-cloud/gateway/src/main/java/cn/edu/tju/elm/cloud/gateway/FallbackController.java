package cn.edu.tju.elm.cloud.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    @PostMapping("/user-service")
    public Mono<Map<String, Object>> userServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", null);
        response.put("message", "用户服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }

    @GetMapping("/merchant-service")
    @PostMapping("/merchant-service")
    public Mono<Map<String, Object>> merchantServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", new ArrayList<>());
        response.put("message", "商家服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }

    @GetMapping("/product-service")
    @PostMapping("/product-service")
    public Mono<Map<String, Object>> productServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", new ArrayList<>());
        response.put("message", "商品服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }

    @GetMapping("/cart-service")
    @PostMapping("/cart-service")
    public Mono<Map<String, Object>> cartServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", new ArrayList<>());
        response.put("message", "购物车服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }

    @GetMapping("/order-service")
    @PostMapping("/order-service")
    public Mono<Map<String, Object>> orderServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", new ArrayList<>());
        response.put("message", "订单服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }

    @GetMapping("/address-service")
    @PostMapping("/address-service")
    public Mono<Map<String, Object>> addressServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", new ArrayList<>());
        response.put("message", "地址服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }

    @GetMapping("/wallet-service")
    @PostMapping("/wallet-service")
    public Mono<Map<String, Object>> walletServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", null);
        response.put("message", "钱包服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }

    @GetMapping("/points-service")
    @PostMapping("/points-service")
    public Mono<Map<String, Object>> pointsServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "OK");
        response.put("data", null);
        response.put("message", "积分服务暂时不可用，请稍后再试");
        return Mono.just(response);
    }
}