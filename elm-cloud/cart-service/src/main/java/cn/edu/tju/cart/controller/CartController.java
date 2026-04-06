package cn.edu.tju.cart.controller;

import cn.edu.tju.cart.model.Cart;
import cn.edu.tju.cart.repository.CartRepository;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.cart.util.JwtUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
// NOTE: server.servlet.context-path is '/elm' (see logs). Do NOT include '/elm' here,
// otherwise routes become '/elm/elm/...'.
@RequestMapping({"/api/carts", "/carts"})
public class CartController {

    private final CartRepository cartRepository;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate = new RestTemplate();

    public CartController(CartRepository cartRepository, JwtUtils jwtUtils) {
        this.cartRepository = cartRepository;
        this.jwtUtils = jwtUtils;
    }

    private String verifyUser(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            return "1"; // Fallback
        }
        return String.valueOf(userId);
    }

    // Debug helper: echo raw body to quickly diagnose JSON parsing issues.
    // IMPORTANT: use byte[] so Spring doesn't try to parse it as JSON via Jackson.
    // Safe because it does not persist anything.
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

    @GetMapping
    public HttpResult<List<Cart>> getCurrentUserCart(@RequestHeader(value = "Authorization", required = false) String token) {
        String userId = verifyUser(token);
        List<Cart> carts = cartRepository.findByUserId(userId);

        // Enrich food details for frontend display (foodName/foodPrice etc).
        // product-service doesn't support batch-get by ids, so we do per-item fetch,
        // but cache by foodId within this request to reduce N+1 duplicates.
        // Fail-soft: if fetch fails, still return cart ids so frontend can proceed.
        Map<String, Map<String, Object>> foodCache = new HashMap<>();
        for (Cart cart : carts) {
            try {
                if (cart.getFoodId() == null) continue;
                Map<String, Object> cached = foodCache.get(cart.getFoodId());
                if (cached != null) {
                    cart.setFoodDetail(cached);
                    continue;
                }

                String url = "http://gateway:8080/elm/api/foods/" + cart.getFoodId();
                ResponseEntity<HttpResult<Object>> resp = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<HttpResult<Object>>() {}
                );
                HttpResult<Object> body = resp.getBody();
                if (body != null && Boolean.TRUE.equals(body.getSuccess()) && body.getData() instanceof Map<?, ?> map) {
                    Map<String, Object> foodView = new HashMap<>();
                    foodView.put("id", cart.getFoodId());
                    foodView.put("foodName", map.get("foodName"));
                    foodView.put("foodPrice", map.get("foodPrice"));
                    foodView.put("foodImg", map.get("foodImg"));
                    foodCache.put(cart.getFoodId(), foodView);
                    cart.setFoodDetail(foodView);
                }
            } catch (Exception ignored) {
                // ignore enrichment failure
            }
        }
        return HttpResult.success(carts);
    }

    @PostMapping
    @Transactional
    public HttpResult<Cart> addCartItem(@RequestHeader(value = "Authorization", required = false) String token, @RequestBody AddCartRequest req) {
        String userId = verifyUser(token);

        // Normalize ids: support both nested objects and plain *_Id fields.
        String foodId = req.getFoodId();
        if (foodId == null && req.getFood() != null) foodId = req.getFood().getId();
        String businessId = req.getBusinessId();
        if (businessId == null && req.getBusiness() != null) businessId = req.getBusiness().getId();

        if (foodId == null || businessId == null) {
            return HttpResult.failure(cn.edu.tju.core.model.ResultCodeEnum.SERVER_ERROR,
                "Missing foodId/businessId");
        }

        int addQty = (req.getQuantity() == null ? 1 : req.getQuantity());

        Cart existingItem = cartRepository.findByUserIdAndBusinessIdAndFoodId(userId, businessId, foodId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + addQty);
            return HttpResult.success(cartRepository.save(existingItem));
        } else {
            Cart created = new Cart();
            created.setUserId(userId);
            created.setBusinessId(businessId);
            created.setFoodId(foodId);
            created.setQuantity(addQty);
            return HttpResult.success(cartRepository.save(created));
        }
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
            return HttpResult.success(cartRepository.save(cart));
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
