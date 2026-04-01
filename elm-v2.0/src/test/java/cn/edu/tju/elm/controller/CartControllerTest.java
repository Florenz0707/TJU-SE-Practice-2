package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {
  @Mock private UserService userService;
  @Mock private InternalOrderClient internalOrderClient;
  @Mock private FoodService foodService;
  @Mock private BusinessService businessService;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private CartController cartController;

  @Test
  void addCartItem_shouldCreateViaInternalOrderClient() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Business business = new Business();
    business.setId(2L);
    Food food = new Food();
    food.setId(1L);
    food.setBusiness(business);
    food.setDeleted(false);
    when(foodService.getFoodById(1L)).thenReturn(food);

    when(internalOrderClient.createCart(any()))
        .thenReturn(new InternalOrderClient.CartSnapshot(100L, 1L, 9L, 2L, 3));
    when(businessService.getBusinessById(2L)).thenReturn(business);

    Cart request = new Cart();
    request.setFood(food);
    request.setQuantity(3);

    var result = cartController.addCartItem(request);

    assertTrue(result.getSuccess());
    verify(internalOrderClient).createCart(any());
    verify(compatibilityEnricher).enrichCart(any(Cart.class));
  }

  @Test
  void getCarts_shouldReadFromInternalOrderClient() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getCartsByCustomerId(9L))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(101L, 1L, 9L, 2L, 2)));

    Food food = new Food();
    food.setId(1L);
    when(foodService.getFoodById(1L)).thenReturn(food);
    Business business = new Business();
    business.setId(2L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    var result = cartController.getCarts();

    assertTrue(result.getSuccess());
    verify(internalOrderClient).getCartsByCustomerId(9L);
    verify(compatibilityEnricher).enrichCarts(any());
  }

  @Test
  void updateCartItem_shouldFailWhenNotOwnerAndNotAdmin() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getCartById(100L))
        .thenReturn(new InternalOrderClient.CartSnapshot(100L, 1L, 10L, 2L, 1));

    Food food = new Food();
    food.setId(1L);
    when(foodService.getFoodById(1L)).thenReturn(food);
    Business business = new Business();
    business.setId(2L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    Cart request = new Cart();
    request.setQuantity(5);

    var result = cartController.updateCartItem(100L, request);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).updateCartQuantity(any(), any());
  }

  @Test
  void deleteCartItem_shouldDeleteWhenOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getCartById(100L))
        .thenReturn(new InternalOrderClient.CartSnapshot(100L, 1L, 9L, 2L, 1));
    when(internalOrderClient.deleteCart(100L)).thenReturn(true);

    Food food = new Food();
    food.setId(1L);
    when(foodService.getFoodById(1L)).thenReturn(food);
    Business business = new Business();
    business.setId(2L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    var result = cartController.deleteCartItem(100L);

    assertTrue(result.getSuccess());
    verify(internalOrderClient).deleteCart(100L);
  }

  @Test
  void updateCartItem_shouldAllowAdminOnNonOwnedCart() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getCartById(100L))
        .thenReturn(new InternalOrderClient.CartSnapshot(100L, 1L, 10L, 2L, 1));
    when(internalOrderClient.updateCartQuantity(100L, 5))
        .thenReturn(new InternalOrderClient.CartSnapshot(100L, 1L, 10L, 2L, 5));

    Food food = new Food();
    food.setId(1L);
    when(foodService.getFoodById(1L)).thenReturn(food);
    Business business = new Business();
    business.setId(2L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    Cart request = new Cart();
    request.setQuantity(5);

    var result = cartController.updateCartItem(100L, request);

    assertTrue(result.getSuccess());
    verify(internalOrderClient).updateCartQuantity(100L, 5);
  }

  @Test
  void updateCartItem_shouldFailWhenRemoteUpdateReturnsNull() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getCartById(100L))
        .thenReturn(new InternalOrderClient.CartSnapshot(100L, 1L, 9L, 2L, 1));
    when(internalOrderClient.updateCartQuantity(100L, 5)).thenReturn(null);

    Food food = new Food();
    food.setId(1L);
    when(foodService.getFoodById(1L)).thenReturn(food);
    Business business = new Business();
    business.setId(2L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    Cart request = new Cart();
    request.setQuantity(5);

    var result = cartController.updateCartItem(100L, request);

    assertFalse(result.getSuccess());
  }

  @Test
  void deleteCartItem_shouldFailWhenRemoteDeleteReturnsFalse() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getCartById(100L))
        .thenReturn(new InternalOrderClient.CartSnapshot(100L, 1L, 10L, 2L, 1));
    when(internalOrderClient.deleteCart(100L)).thenReturn(false);

    Food food = new Food();
    food.setId(1L);
    when(foodService.getFoodById(1L)).thenReturn(food);
    Business business = new Business();
    business.setId(2L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    var result = cartController.deleteCartItem(100L);

    assertFalse(result.getSuccess());
    verify(internalOrderClient).deleteCart(100L);
  }
}
