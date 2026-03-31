package cn.edu.tju.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.cart.model.bo.Cart;
import cn.edu.tju.cart.model.vo.CartSnapshotVO;
import cn.edu.tju.cart.service.CartInternalService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CartInnerControllerTest {

  @Mock private CartInternalService cartInternalService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new CartInnerController(cartInternalService)).build();
  }

  @Test
  void createCartRejectsMissingFields() throws Exception {
    mockMvc
        .perform(
            post("/api/inner/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodId\":1,\"customerId\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(40400))
        .andExpect(jsonPath("$.message").value("Cart fields CANT BE NULL"));
  }

  @Test
  void updateCartQuantityReturnsUpdatedSnapshot() throws Exception {
    when(cartInternalService.updateCartQuantity(5L, 3)).thenReturn(snapshot(5L, 9L, 2L, 7L, 3));

    mockMvc
        .perform(
            post("/api/inner/cart/5/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(5))
        .andExpect(jsonPath("$.data.quantity").value(3));
  }

  @Test
  void getCartsByBusinessAndCustomerIdReturnsList() throws Exception {
    when(cartInternalService.getCartsByBusinessAndCustomerId(eq(7L), eq(2L)))
        .thenReturn(List.of(snapshot(1L, 9L, 2L, 7L, 1), snapshot(2L, 10L, 2L, 7L, 4)));

    mockMvc
        .perform(get("/api/inner/cart/business/7/customer/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[1].quantity").value(4));
  }

  @Test
  void createCartReturnsServerErrorWhenServiceThrows() throws Exception {
    when(cartInternalService.createCart(any())).thenThrow(new IllegalStateException("duplicate cart"));

    mockMvc
        .perform(
            post("/api/inner/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodId\":1,\"customerId\":2,\"businessId\":3,\"quantity\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(50000))
        .andExpect(jsonPath("$.message").value("duplicate cart"));
  }

  private static CartSnapshotVO snapshot(Long id, Long foodId, Long customerId, Long businessId, Integer quantity) {
    Cart cart = new Cart();
    cart.setId(id);
    cart.setFoodId(foodId);
    cart.setCustomerId(customerId);
    cart.setBusinessId(businessId);
    cart.setQuantity(quantity);
    return new CartSnapshotVO(cart);
  }
}