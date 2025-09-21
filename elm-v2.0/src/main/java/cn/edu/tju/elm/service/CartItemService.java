package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Cart;
import cn.edu.tju.elm.repository.CartItemRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<Cart> getCart(Long businessId, Long customerId) {
        return Utils.removeDeleted(cartItemRepository.findAllByBusinessIdAndCustomerId(businessId, customerId));
    }

    public void updateCart(Cart cart) {
        cartItemRepository.save(cart);
    }
}
