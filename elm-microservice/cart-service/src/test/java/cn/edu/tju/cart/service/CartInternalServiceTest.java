package cn.edu.tju.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.cart.model.bo.Cart;
import cn.edu.tju.cart.model.vo.CartSnapshotVO;
import cn.edu.tju.cart.repository.CartRepository;
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
  void createCartSavesAndReturnsSnapshot() {
    Cart saved = createCart(1L, 10L, 20L, 30L, 2);
    when(cartRepository.save(any(Cart.class))).thenReturn(saved);

    CartSnapshotVO result =
        cartInternalService.createCart(new CartInternalService.CreateCartCommand(10L, 20L, 30L, 2));

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getFoodId()).isEqualTo(10L);
    assertThat(result.getCustomerId()).isEqualTo(20L);
    assertThat(result.getBusinessId()).isEqualTo(30L);
    assertThat(result.getQuantity()).isEqualTo(2);
    verify(cartRepository).save(any(Cart.class));
  }

  @Test
  void createCartThrowsWhenRequiredFieldMissing() {
    assertThatThrownBy(
            () -> cartInternalService.createCart(new CartInternalService.CreateCartCommand(null, 20L, 30L, 2)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cart required fields CANT BE NULL");
  }

  @Test
  void updateCartQuantityUpdatesValue() {
    Cart cart = createCart(2L, 11L, 21L, 31L, 1);
    when(cartRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(cart));
    when(cartRepository.save(cart)).thenReturn(cart);

    CartSnapshotVO result = cartInternalService.updateCartQuantity(2L, 5);

    assertThat(result.getQuantity()).isEqualTo(5);
    assertThat(cart.getQuantity()).isEqualTo(5);
    verify(cartRepository).save(cart);
  }

  @Test
  void getCartsByCustomerIdReturnsSnapshots() {
    when(cartRepository.findAllByCustomerIdAndDeletedFalse(88L))
        .thenReturn(List.of(createCart(3L, 12L, 88L, 32L, 1), createCart(4L, 13L, 88L, 33L, 2)));

    List<CartSnapshotVO> result = cartInternalService.getCartsByCustomerId(88L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCustomerId()).isEqualTo(88L);
    assertThat(result.get(1).getCustomerId()).isEqualTo(88L);
  }

  @Test
  void deleteCartDeletesExistingCart() {
    Cart cart = createCart(5L, 15L, 25L, 35L, 3);
    when(cartRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(cart));

    boolean deleted = cartInternalService.deleteCart(5L);

    assertThat(deleted).isTrue();
    verify(cartRepository).delete(cart);
  }

  private Cart createCart(Long id, Long foodId, Long customerId, Long businessId, Integer quantity) {
    Cart cart = new Cart();
    cart.setId(id);
    cart.setFoodId(foodId);
    cart.setCustomerId(customerId);
    cart.setBusinessId(businessId);
    cart.setQuantity(quantity);
    cart.setDeleted(false);
    return cart;
  }
}