package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.elm.service.OrderApplicationService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.InternalOrderClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodControllerTest {

  @Mock private UserService userService;
  @Mock private FoodService foodService;
  @Mock private BusinessService businessService;
  @Mock private OrderApplicationService orderApplicationService;
  @Mock private InternalOrderClient internalOrderClient;

  @InjectMocks private FoodController foodController;

  @Test
  void getAllFoods_shouldFailWhenArgumentsInvalid() {
    var result = foodController.getAllFoods(null, null);

    assertFalse(result.getSuccess());
    assertEquals("HAVE TO PROVIDE ONE AND ONLY ONE ARG", result.getMessage());
  }

  @Test
  void getAllFoods_shouldFailWhenBusinessMissing() {
    when(businessService.getBusinessById(100L)).thenReturn(null);

    var result = foodController.getAllFoods(100L, null);

    assertFalse(result.getSuccess());
    assertEquals("Business NOT FOUND", result.getMessage());
  }

  @Test
  void getAllFoods_shouldCollectFoodsFromOrderDetails() {
    Order order = new Order();
    order.setId(200L);
    when(orderApplicationService.getOrderById(200L)).thenReturn(order);
    Food food = new Food();
    food.setId(10L);
    food.setFoodName("Rice");
    when(internalOrderClient.getOrderDetailsByOrderId(200L))
        .thenReturn(
            List.of(
                new InternalOrderClient.OrderDetailSnapshot(1L, 200L, 10L, 1),
                new InternalOrderClient.OrderDetailSnapshot(2L, 200L, null, 1),
                new InternalOrderClient.OrderDetailSnapshot(3L, 200L, 11L, 1)));
    when(foodService.getFoodById(10L)).thenReturn(food);
    when(foodService.getFoodById(11L)).thenReturn(null);

    var result = foodController.getAllFoods(null, 200L);

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
    assertEquals("Rice", result.getData().getFirst().getFoodName());
  }

  @Test
  void addFood_shouldFailWhenBusinessOwnerUnauthorized() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setId(100L);
    business.setBusinessOwnerId(10L);
    when(businessService.getBusinessById(100L)).thenReturn(business);
    Food request = new Food();
    request.setFoodName("Rice");
    request.setFoodPrice(new BigDecimal("10"));
    Business requestBusiness = new Business();
    requestBusiness.setId(100L);
    request.setBusiness(requestBusiness);

    var result = foodController.addFood(request);

    assertFalse(result.getSuccess());
    verify(foodService, never()).addFood(request);
  }

  @Test
  void updateFood_shouldFillMissingFieldsAndReplaceEntity() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setBusinessOwnerId(9L);
    Food existing = new Food();
    existing.setId(100L);
    existing.setFoodName("Rice");
    existing.setFoodPrice(new BigDecimal("10"));
    existing.setFoodExplain("old explain");
    existing.setBusiness(business);
    when(foodService.getFoodById(100L)).thenReturn(existing);
    Food patch = new Food();
    patch.setRemarks("new remarks");

    var result = foodController.updateFood(100L, patch);

    assertTrue(result.getSuccess());
    assertEquals("Rice", patch.getFoodName());
    assertEquals(new BigDecimal("10"), patch.getFoodPrice());
    assertEquals("old explain", patch.getFoodExplain());
    verify(foodService).updateFood(same(existing));
    verify(foodService).updateFood(same(patch));
  }

  @Test
  void deleteFood_shouldSoftDeleteForOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setBusinessOwnerId(9L);
    Food food = new Food();
    food.setId(100L);
    food.setBusiness(business);
    food.setDeleted(false);
    when(foodService.getFoodById(100L)).thenReturn(food);

    var result = foodController.deleteFood(100L);

    assertTrue(result.getSuccess());
    assertEquals("Delete food successfully.", result.getData());
    assertTrue(Boolean.TRUE.equals(food.getDeleted()));
    verify(foodService).updateFood(food);
  }
}