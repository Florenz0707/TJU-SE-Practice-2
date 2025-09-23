package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Cart;
import cn.edu.tju.elm.repository.CartItemRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartItemService {

    private final CartItemRepository cartItemRepository;

    public CartItemService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    public void addCart(Cart cart) {
        cartItemRepository.save(cart);
    }

    public List<Cart> getCart(Long businessId, Long customerId) {
        return Utils.checkEntityList(cartItemRepository.findAllByBusinessIdAndCustomerId(businessId, customerId));
    }

    public List<Cart> getUserCarts(Long customerId) {
        return Utils.checkEntityList(cartItemRepository.findAllByCustomerId(customerId));
    }

    public Cart getCartById(Long cartId) {
        Optional<Cart> cartOptional = cartItemRepository.findById(cartId);
        return cartOptional.map(Utils::checkEntity).orElse(null);
    }

    public void updateCart(Cart cart) {
        cartItemRepository.save(cart);
    }
}
