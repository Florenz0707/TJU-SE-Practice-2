package cn.edu.tju.cart.controller;

import cn.edu.tju.cart.model.Cart;
import cn.edu.tju.cart.repository.CartRepository;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.cart.util.JwtUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartRepository cartRepository;
    private final JwtUtils jwtUtils;

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

    @GetMapping
    public HttpResult<List<Cart>> getCurrentUserCart(@RequestHeader(value = "Authorization", required = false) String token) {
        String userId = verifyUser(token);
        List<Cart> carts = cartRepository.findByUserId(userId);
        return HttpResult.success(carts);
    }

    @PostMapping
    @Transactional
    public HttpResult<Cart> addCartItem(@RequestHeader(value = "Authorization", required = false) String token, @RequestBody Cart cartRequest) {
        String userId = verifyUser(token);
        Cart existingItem = cartRepository.findByUserIdAndBusinessIdAndFoodId(
                userId, cartRequest.getBusinessId(), cartRequest.getFoodId()
        );
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + cartRequest.getQuantity());
            return HttpResult.success(cartRepository.save(existingItem));
        } else {
            cartRequest.setUserId(userId);
            return HttpResult.success(cartRepository.save(cartRequest));
        }
    }

    @PatchMapping("/{id}")
    @Transactional
    public HttpResult<Cart> updateCartItem(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable("id") String id, @RequestBody Cart updateRequest) {
        Cart cart = cartRepository.findById(id).orElse(null);
        if (cart != null) {
            cart.setQuantity(updateRequest.getQuantity());
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
