package cn.edu.tju.cart.service.impl;

import cn.edu.tju.cart.model.Cart;
import cn.edu.tju.cart.repository.CartRepository;
import cn.edu.tju.cart.service.CartInternalService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartInternalServiceImpl implements CartInternalService {

    private final CartRepository cartRepository;

    public CartInternalServiceImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public Optional<Cart> getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }
}
