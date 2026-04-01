package cn.edu.tju.cart.service;

import cn.edu.tju.cart.model.Cart;

import java.util.Optional;

public interface CartInternalService {
    Optional<Cart> getCartByUserId(Long userId);
}
