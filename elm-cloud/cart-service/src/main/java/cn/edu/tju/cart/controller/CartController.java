package cn.edu.tju.cart.controller;

import cn.edu.tju.cart.model.Cart;
import cn.edu.tju.cart.repository.CartRepository;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.cart.util.JwtUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping({"/api/carts", "/carts"})
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    private static final String PRODUCT_SERVICE_CB = "productService";
    private static final String MERCHANT_SERVICE_CB = "merchantService";

    private final CartRepository cartRepository;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;

    public CartController(CartRepository cartRepository, JwtUtils jwtUtils, RestTemplate restTemplate) {
        this.cartRepository = cartRepository;
        this.jwtUtils = jwtUtils;
        this.restTemplate = restTemplate;
    }

    private String verifyUser(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            return "1";
        }
        return String.valueOf(userId);
    }

    @PostMapping(
        value = "/_debug/echo",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String echo(@RequestBody(required = false) byte[] body) {
        if (body == null) return "<null>";
        return new String(body, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddCartRequest {
        private Cart.IdOnlyView food;
        private Cart.IdOnlyView business;
        private Cart.IdOnlyView customer;
        private String foodId;
        private String businessId;
        private String userId;
        private Integer quantity;

        public Cart.IdOnlyView getFood() { return food; }
        public void setFood(Cart.IdOnlyView food) { this.food = food; }
        public Cart.IdOnlyView getBusiness() { return business; }
        public void setBusiness(Cart.IdOnlyView business) { this.business = business; }
        public Cart.IdOnlyView getCustomer() { return customer; }
        public void setCustomer(Cart.IdOnlyView customer) { this.customer = customer; }
        public String getFoodId() { return foodId; }
        public void setFoodId(String foodId) { this.foodId = foodId; }
        public String getBusinessId() { return businessId; }
        public void setBusinessId(String businessId) { this.businessId = businessId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateCartRequest {
        private Integer quantity;
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    private void enrichCart(Cart cart, Map<String, Map<String, Object>> foodCache, 
                           Map<String, Map<String, Object>> businessCache, 
                           Map<String, Map<String, Object>> customerCache) {
        try {
            if (cart.getFoodId() != null) {
                Map<String, Object> cachedFood = foodCache.get(cart.getFoodId());
                if (cachedFood != null) {
                    cart.setFoodDetail(cachedFood);
                } else {
                    getFoodFromService(cart, foodCache);
                }
            }
            
            if (cart.getBusinessId() != null) {
                Map<String, Object> cachedBusiness = businessCache.get(cart.getBusinessId());
                if (cachedBusiness != null) {
                    cart.setBusiness(cachedBusiness);
                } else {
                    getBusinessFromService(cart, businessCache);
                }
            }
            
            if (cart.getUserId() != null) {
                Map<String, Object> cachedCustomer = customerCache.get(cart.getUserId());
                if (cachedCustomer != null) {
                    cart.setCustomer(cachedCustomer);
                } else {
                    Map<String, Object> customerView = new HashMap<>();
                    customerView.put("id", cart.getUserId());
                    customerView.put("username", "用户" + cart.getUserId());
                    customerCache.put(cart.getUserId(), customerView);
                    cart.setCustomer(customerView);
                }
            }
        } catch (Exception ignored) {
            log.warn("Enrich cart failed: {}", ignored.getMessage());
        }
    }

    @CircuitBreaker(name = PRODUCT_SERVICE_CB, fallbackMethod = "getFoodFromServiceFallback")
    private void getFoodFromService(Cart cart, Map<String, Map<String, Object>> foodCache) {
        String foodUrl = "http://product-service/elm/api/foods/" + cart.getFoodId();
        ResponseEntity<HttpResult<Object>> foodResp = restTemplate.exchange(
                foodUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<HttpResult<Object>>() {}
        );
        HttpResult<Object> foodBody = foodResp.getBody();
        if (foodBody != null && Boolean.TRUE.equals(foodBody.getSuccess()) && foodBody.getData() instanceof Map<?, ?> map) {
            Map<String, Object> foodView = new HashMap<>();
            foodView.put("id", cart.getFoodId());
            foodView.put("foodName", map.get("foodName"));
            foodView.put("foodPrice", map.get("foodPrice"));
            foodView.put("foodImg", map.get("foodImg"));
            foodView.put("foodExplain", map.get("foodExplain"));
            foodCache.put(cart.getFoodId(), foodView);
            cart.setFoodDetail(foodView);
        }
    }

    private void getFoodFromServiceFallback(Cart cart, Map<String, Map<String, Object>> foodCache, Exception e) {
        log.warn("Fallback triggered for getFoodFromService: {}", e.getMessage());
    }

    @CircuitBreaker(name = MERCHANT_SERVICE_CB, fallbackMethod = "getBusinessFromServiceFallback")
    private void getBusinessFromService(Cart cart, Map<String, Map<String, Object>> businessCache) {
        String businessUrl = "http://merchant-service/elm/api/businesses/" + cart.getBusinessId();
        ResponseEntity<HttpResult<Object>> businessResp = restTemplate.exchange(
                businessUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<HttpResult<Object>>() {}
        );
        HttpResult<Object> businessBody = businessResp.getBody();
        if (businessBody != null && Boolean.TRUE.equals(businessBody.getSuccess()) && businessBody.getData() instanceof Map<?, ?> map) {
            Map<String, Object> businessView = new HashMap<>();
            businessView.put("id", cart.getBusinessId());
            businessView.put("businessName", map.get("businessName"));
            businessView.put("businessAddress", map.get("businessAddress"));
            businessView.put("businessExplain", map.get("businessExplain"));
            businessView.put("businessImg", map.get("businessImg"));
            businessView.put("deliveryPrice", map.get("deliveryPrice"));
            businessView.put("startPrice", map.get("startPrice"));
            businessCache.put(cart.getBusinessId(), businessView);
            cart.setBusiness(businessView);
        }
    }

    private void getBusinessFromServiceFallback(Cart cart, Map<String, Map<String, Object>> businessCache, Exception e) {
        log.warn("Fallback triggered for getBusinessFromService: {}", e.getMessage());
    }

    @GetMapping
    public HttpResult<List<Cart>> getCurrentUserCart(@RequestHeader(value = "Authorization", required = false) String token) {
        String userId = verifyUser(token);
        List<Cart> carts = cartRepository.findByUserId(userId);

        Map<String, Map<String, Object>> foodCache = new HashMap<>();
        Map<String, Map<String, Object>> businessCache = new HashMap<>();
        Map<String, Map<String, Object>> customerCache = new HashMap<>();
        
        for (Cart cart : carts) {
            enrichCart(cart, foodCache, businessCache, customerCache);
        }
        return HttpResult.success(carts);
    }

    @PostMapping
    @Transactional
    public HttpResult<Cart> addCartItem(@RequestHeader(value = "Authorization", required = false) String token, @RequestBody AddCartRequest req) {
        String userId = verifyUser(token);

        String foodId = req.getFoodId();
        if (foodId == null && req.getFood() != null) foodId = req.getFood().getId();
        String businessId = req.getBusinessId();
        if (businessId == null && req.getBusiness() != null) businessId = req.getBusiness().getId();

        if (foodId == null || businessId == null) {
            return HttpResult.failure(cn.edu.tju.core.model.ResultCodeEnum.SERVER_ERROR,
                "Missing foodId/businessId");
        }

        int addQty = (req.getQuantity() == null ? 1 : req.getQuantity());

        Cart savedCart;
        Cart existingItem = cartRepository.findByUserIdAndBusinessIdAndFoodId(userId, businessId, foodId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + addQty);
            savedCart = cartRepository.save(existingItem);
        } else {
            Cart created = new Cart();
            created.setUserId(userId);
            created.setBusinessId(businessId);
            created.setFoodId(foodId);
            created.setQuantity(addQty);
            savedCart = cartRepository.save(created);
        }

        Map<String, Map<String, Object>> foodCache = new HashMap<>();
        Map<String, Map<String, Object>> businessCache = new HashMap<>();
        Map<String, Map<String, Object>> customerCache = new HashMap<>();
        enrichCart(savedCart, foodCache, businessCache, customerCache);
        
        return HttpResult.success(savedCart);
    }

    @PatchMapping("/{id}")
    @Transactional
    public HttpResult<Cart> updateCartItem(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable("id") String id, @RequestBody UpdateCartRequest updateRequest) {
        String userId = verifyUser(token);
        Cart cart = cartRepository.findById(id).orElse(null);
        if (cart != null && cart.getUserId() != null && cart.getUserId().equals(userId)) {
            Integer quantity = updateRequest.getQuantity();
            if (quantity == null) {
                return HttpResult.failure(cn.edu.tju.core.model.ResultCodeEnum.SERVER_ERROR, "Missing quantity");
            }
            cart.setQuantity(quantity);
            Cart savedCart = cartRepository.save(cart);
            
            Map<String, Map<String, Object>> foodCache = new HashMap<>();
            Map<String, Map<String, Object>> businessCache = new HashMap<>();
            Map<String, Map<String, Object>> customerCache = new HashMap<>();
            enrichCart(savedCart, foodCache, businessCache, customerCache);
            
            return HttpResult.success(savedCart);
        }
        return HttpResult.failure(cn.edu.tju.core.model.ResultCodeEnum.NOT_FOUND, "Cart item not found");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public HttpResult<String> deleteCartItem(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable("id") String id) {
        String userId = verifyUser(token);
        Cart cart = cartRepository.findById(id).orElse(null);
        if (cart != null && cart.getUserId().equals(userId)) {
            cartRepository.delete(cart);
        }
        return HttpResult.success("deleted");
    }
}
