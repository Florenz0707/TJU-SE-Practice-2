package cn.edu.tju.cart.service.impl;

import cn.edu.tju.cart.model.Cart;
import cn.edu.tju.cart.repository.CartRepository;
import cn.edu.tju.cart.service.CartInternalService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartInternalServiceImpl implements CartInternalService {

    private final CartRepository cartRepository;

    public CartInternalServiceImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public List<Cart> getCartByUserId(String userId) {
        return cartRepository.findByUserId(userId);
    }
}
