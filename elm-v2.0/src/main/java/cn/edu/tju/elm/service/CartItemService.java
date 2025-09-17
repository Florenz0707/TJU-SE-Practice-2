package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Cart;
import cn.edu.tju.elm.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartItemService {

    private final CartItemRepository cartItemRepository;

    public CartItemService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    public Cart addCart(Cart cart) {
        return cartItemRepository.save(cart);
    }
}
