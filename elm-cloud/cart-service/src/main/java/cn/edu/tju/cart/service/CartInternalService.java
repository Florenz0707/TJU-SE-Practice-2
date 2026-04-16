package cn.edu.tju.cart.service;

import cn.edu.tju.cart.model.Cart;

import java.util.List;

public interface CartInternalService {
    List<Cart> getCartByUserId(String userId);
    void clearCartByUserId(String userId);
}
