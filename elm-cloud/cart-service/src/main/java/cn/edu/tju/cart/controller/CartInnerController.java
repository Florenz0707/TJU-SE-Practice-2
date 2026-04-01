package cn.edu.tju.cart.controller;

import cn.edu.tju.cart.model.Cart;
import cn.edu.tju.cart.service.CartInternalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/carts")
public class CartInnerController {

    private final CartInternalService cartInternalService;

    public CartInnerController(CartInternalService cartInternalService) {
        this.cartInternalService = cartInternalService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Cart> getByUser(@PathVariable Long userId) {
        return cartInternalService.getCartByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
