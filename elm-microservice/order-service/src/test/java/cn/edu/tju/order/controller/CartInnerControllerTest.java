package cn.edu.tju.order.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.model.bo.Cart;
import cn.edu.tju.order.model.vo.CartSnapshotVO;
import cn.edu.tju.order.service.CartInternalService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartInnerControllerTest {
  @Mock private CartInternalService cartInternalService;

  @InjectMocks private CartInnerController cartInnerController;

  @Test
  void createCart_shouldFailWhenMissingField() {
    CartInnerController.CreateCartRequest request = new CartInnerController.CreateCartRequest();
    var result = cartInnerController.createCart(request);
    assertFalse(result.getSuccess());
  }

  @Test
  void getCartsByCustomerId_shouldReturnData() {
    when(cartInternalService.getCartsByCustomerId(9L)).thenReturn(List.of());
    var result = cartInnerController.getCartsByCustomerId(9L);
    assertTrue(result.getSuccess());
  }

  @Test
  void updateCartQuantity_shouldFailWhenQuantityMissing() {
    var result =
        cartInnerController.updateCartQuantity(
            1L, new CartInnerController.UpdateCartQuantityRequest());
    assertFalse(result.getSuccess());
  }

  @Test
  void getCartById_shouldReturnData() {
    Cart cart = new Cart();
    cart.setId(1L);
    when(cartInternalService.getCartById(1L)).thenReturn(new CartSnapshotVO(cart));
    var result = cartInnerController.getCartById(1L);
    assertTrue(result.getSuccess());
  }
}
