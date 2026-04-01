package cn.edu.tju.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.model.bo.Cart;
import cn.edu.tju.order.repository.CartRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartInternalServiceTest {
  @Mock private CartRepository cartRepository;

  @InjectMocks private CartInternalService cartInternalService;

  @Test
  void createCart_shouldReturnCreatedSnapshot() {
    Cart saved = new Cart();
    saved.setId(1L);
    saved.setFoodId(10L);
    saved.setBusinessId(20L);
    saved.setCustomerId(30L);
    saved.setQuantity(2);
    when(cartRepository.save(org.mockito.ArgumentMatchers.any(Cart.class))).thenReturn(saved);

    var result =
        cartInternalService.createCart(new CartInternalService.CreateCartCommand(10L, 30L, 20L, 2));

    assertEquals(1L, result.getId());
    assertEquals(10L, result.getFoodId());
  }

  @Test
  void getCartsByBusinessAndCustomerId_shouldReturnList() {
    Cart cart = new Cart();
    cart.setId(1L);
    when(cartRepository.findAllByBusinessIdAndCustomerIdAndDeletedFalse(20L, 30L))
        .thenReturn(List.of(cart));

    var result = cartInternalService.getCartsByBusinessAndCustomerId(20L, 30L);

    assertEquals(1, result.size());
  }

  @Test
  void updateCartQuantity_shouldThrowWhenMissing() {
    when(cartRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
    assertThrows(
        IllegalArgumentException.class, () -> cartInternalService.updateCartQuantity(1L, 2));
  }

  @Test
  void deleteCart_shouldReturnTrueWhenExists() {
    Cart cart = new Cart();
    cart.setId(1L);
    when(cartRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cart));

    boolean result = cartInternalService.deleteCart(1L);

    assertTrue(result);
  }
}
