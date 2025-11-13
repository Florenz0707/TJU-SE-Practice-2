package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.repository.CartItemRepository;
import cn.edu.tju.elm.utils.EntityUtils;
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
        return EntityUtils.filterEntityList(cartItemRepository.findAllByBusinessIdAndCustomerId(businessId, customerId));
    }

    public List<Cart> getUserCarts(Long customerId) {
        return EntityUtils.filterEntityList(cartItemRepository.findAllByCustomerId(customerId));
    }

    public Cart getCartById(Long cartId) {
        Optional<Cart> cartOptional = cartItemRepository.findById(cartId);
        return cartOptional.map(EntityUtils::filterEntity).orElse(null);
    }

    public void updateCart(Cart cart) {
        cartItemRepository.save(cart);
    }

    public void deleteCart(Cart cart) {
        cartItemRepository.delete(cart);
    }
}
