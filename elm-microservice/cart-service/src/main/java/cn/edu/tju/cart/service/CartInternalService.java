package cn.edu.tju.cart.service;

import cn.edu.tju.cart.model.bo.Cart;
import cn.edu.tju.cart.model.vo.CartSnapshotVO;
import cn.edu.tju.cart.repository.CartRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartInternalService {
  private final CartRepository cartRepository;

  public CartInternalService(CartRepository cartRepository) {
    this.cartRepository = cartRepository;
  }

  @Transactional
  public CartSnapshotVO createCart(CreateCartCommand command) {
    if (command == null
        || command.foodId() == null
        || command.customerId() == null
        || command.businessId() == null
        || command.quantity() == null) {
      throw new IllegalArgumentException("cart required fields CANT BE NULL");
    }
    Cart cart = new Cart();
    LocalDateTime now = LocalDateTime.now();
    cart.setCreateTime(now);
    cart.setUpdateTime(now);
    cart.setDeleted(false);
    cart.setFoodId(command.foodId());
    cart.setCustomerId(command.customerId());
    cart.setBusinessId(command.businessId());
    cart.setQuantity(command.quantity());
    Cart saved = cartRepository.save(cart);
    return new CartSnapshotVO(saved);
  }

  @Transactional(readOnly = true)
  public CartSnapshotVO getCartById(Long cartId) {
    if (cartId == null) {
      return null;
    }
    return cartRepository.findByIdAndDeletedFalse(cartId).map(CartSnapshotVO::new).orElse(null);
  }

  @Transactional(readOnly = true)
  public List<CartSnapshotVO> getCartsByCustomerId(Long customerId) {
    if (customerId == null) {
      return List.of();
    }
    return cartRepository.findAllByCustomerIdAndDeletedFalse(customerId).stream().map(CartSnapshotVO::new).toList();
  }

  @Transactional(readOnly = true)
  public List<CartSnapshotVO> getCartsByBusinessAndCustomerId(Long businessId, Long customerId) {
    if (businessId == null || customerId == null) {
      return List.of();
    }
    return cartRepository.findAllByBusinessIdAndCustomerIdAndDeletedFalse(businessId, customerId).stream().map(CartSnapshotVO::new).toList();
  }

  @Transactional
  public CartSnapshotVO updateCartQuantity(Long cartId, Integer quantity) {
    if (cartId == null || quantity == null) {
      throw new IllegalArgumentException("cartId/quantity CANT BE NULL");
    }
    Cart cart = cartRepository.findByIdAndDeletedFalse(cartId).orElseThrow(() -> new IllegalArgumentException("Cart NOT FOUND"));
    cart.setQuantity(quantity);
    cart.setUpdateTime(LocalDateTime.now());
    Cart saved = cartRepository.save(cart);
    return new CartSnapshotVO(saved);
  }

  @Transactional
  public boolean deleteCart(Long cartId) {
    if (cartId == null) {
      return false;
    }
    Cart cart = cartRepository.findByIdAndDeletedFalse(cartId).orElseThrow(() -> new IllegalArgumentException("Cart NOT FOUND"));
    cartRepository.delete(cart);
    return true;
  }

  public record CreateCartCommand(Long foodId, Long customerId, Long businessId, Integer quantity) {}
}
