package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.repository.CartItemRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartItemService {

  private final CartItemRepository cartItemRepository;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public CartItemService(
      CartItemRepository cartItemRepository, ResponseCompatibilityEnricher compatibilityEnricher) {
    this.cartItemRepository = cartItemRepository;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public void addCart(Cart cart) {
    cartItemRepository.save(cart);
  }

  public List<Cart> getCart(Long businessId, Long customerId) {
    List<Cart> carts =
        EntityUtils.filterEntityList(
            cartItemRepository.findAllByBusinessIdAndCustomerId(businessId, customerId));
    compatibilityEnricher.enrichCarts(carts);
    return carts;
  }

  public List<Cart> getUserCarts(Long customerId) {
    List<Cart> carts =
        EntityUtils.filterEntityList(cartItemRepository.findAllByCustomerId(customerId));
    compatibilityEnricher.enrichCarts(carts);
    return carts;
  }

  public Cart getCartById(Long cartId) {
    Optional<Cart> cartOptional = cartItemRepository.findById(cartId);
    Cart cart = cartOptional.map(EntityUtils::filterEntity).orElse(null);
    compatibilityEnricher.enrichCart(cart);
    return cart;
  }

  public void updateCart(Cart cart) {
    cartItemRepository.save(cart);
  }

  public void deleteCart(Cart cart) {
    cartItemRepository.delete(cart);
  }
}
