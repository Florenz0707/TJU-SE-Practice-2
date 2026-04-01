package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.OrderApplicationService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

  @Mock private UserService userService;
  @Mock private BusinessService businessService;
  @Mock private OrderApplicationService orderApplicationService;

  @InjectMocks private OrderController orderController;

  @Test
  void getMyOrdersPage_shouldFailWhenInvalidPageSize() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = orderController.getMyOrdersPage(0, 10);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrdersByCustomerId(me.getId(), 0, 10);
  }

  @Test
  void getMyOrdersPage_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = orderController.getMyOrdersPage(1, 10);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrdersByCustomerId(9L, 1, 10);
  }

  @Test
  void getMyOrdersPage_shouldReturnPagedDataWhenAuthorized() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(orderApplicationService.getOrdersByCustomerId(9L, 1, 10))
        .thenReturn(Map.of("records", java.util.List.of(), "total", 0L, "page", 1, "size", 10));

    var result = orderController.getMyOrdersPage(1, 10);

    assertTrue(result.getSuccess());
    verify(orderApplicationService).getOrdersByCustomerId(9L, 1, 10);
  }

  @Test
  void getOrdersByBusinessIdPage_shouldFailWhenNotBusinessOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setId(100L);
    business.setBusinessOwnerId(10L);
    when(businessService.getBusinessById(100L)).thenReturn(business);

    var result = orderController.getOrdersByBusinessIdPage(100L, 1, 10);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrdersByBusinessId(100L, 1, 10);
  }

  @Test
  void getOrdersByBusinessIdPage_shouldReturnDataWhenBusinessOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setId(100L);
    business.setBusinessOwnerId(9L);
    when(businessService.getBusinessById(100L)).thenReturn(business);
    when(orderApplicationService.getOrdersByBusinessId(100L, 1, 10))
        .thenReturn(Map.of("records", java.util.List.of(), "total", 0L, "page", 1, "size", 10));

    var result = orderController.getOrdersByBusinessIdPage(100L, 1, 10);

    assertTrue(result.getSuccess());
    verify(orderApplicationService).getOrdersByBusinessId(100L, 1, 10);
  }

  @Test
  void getOrdersByBusinessIdPage_shouldReturnDataForAdmin() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Business business = new Business();
    business.setId(100L);
    business.setBusinessOwnerId(10L);
    when(businessService.getBusinessById(100L)).thenReturn(business);
    when(orderApplicationService.getOrdersByBusinessId(100L, 1, 10))
        .thenReturn(Map.of("records", java.util.List.of(), "total", 0L, "page", 1, "size", 10));

    var result = orderController.getOrdersByBusinessIdPage(100L, 1, 10);

    assertTrue(result.getSuccess());
    verify(orderApplicationService).getOrdersByBusinessId(100L, 1, 10);
  }

  @Test
  void getOrdersByBusinessIdPage_shouldFailWhenBusinessNotFound() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(businessService.getBusinessById(100L)).thenReturn(null);

    var result = orderController.getOrdersByBusinessIdPage(100L, 1, 10);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrdersByBusinessId(100L, 1, 10);
  }

  @Test
  void getOrdersByBusinessIdPage_shouldFailWhenInvalidPageSize() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = orderController.getOrdersByBusinessIdPage(100L, 0, 10);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrdersByBusinessId(100L, 0, 10);
    verify(businessService, never()).getBusinessById(100L);
  }

  @Test
  void getOrderById_shouldAllowAdmin() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Order order = new Order();
    order.setId(200L);
    order.setCustomerId(10L);
    when(orderApplicationService.getOrderById(200L)).thenReturn(order);

    var result = orderController.getOrderById(200L);

    assertTrue(result.getSuccess());
  }

  @Test
  void getOrderById_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = orderController.getOrderById(200L);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrderById(200L);
  }

  @Test
  void getOrderById_shouldFailWhenOrderNotFound() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(orderApplicationService.getOrderById(200L)).thenReturn(null);

    var result = orderController.getOrderById(200L);

    assertFalse(result.getSuccess());
  }

  @Test
  void getOrderById_shouldFailWhenNonOwnerUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Order order = new Order();
    order.setId(201L);
    order.setCustomerId(10L);
    when(orderApplicationService.getOrderById(201L)).thenReturn(order);

    var result = orderController.getOrderById(201L);

    assertFalse(result.getSuccess());
  }

  @Test
  void listOrdersByUserId_shouldFailWhenNonAdminNonSelf() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = orderController.listOrdersByUserId(10L);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrdersByCustomerId(10L);
  }

  @Test
  void listOrdersByUserId_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = orderController.listOrdersByUserId(10L);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).getOrdersByCustomerId(10L);
  }

  @Test
  void updateOrderStatus_shouldPassRoleFlags() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Order request = new Order();
    request.setId(300L);
    request.setOrderState(3);
    when(orderApplicationService.updateOrderStatus(9L, false, true, request))
        .thenReturn(cn.edu.tju.core.model.HttpResult.success(request));

    var result = orderController.updateOrderStatus(request);

    assertTrue(result.getSuccess());
    verify(orderApplicationService).updateOrderStatus(9L, false, true, request);
  }

  @Test
  void updateOrderStatus_shouldPassAdminFlags() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Order request = new Order();
    request.setId(301L);
    request.setOrderState(4);
    when(orderApplicationService.updateOrderStatus(9L, true, false, request))
        .thenReturn(cn.edu.tju.core.model.HttpResult.success(request));

    var result = orderController.updateOrderStatus(request);

    assertTrue(result.getSuccess());
    verify(orderApplicationService).updateOrderStatus(9L, true, false, request);
  }

  @Test
  void updateOrderStatus_shouldPassUserFlags() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Order request = new Order();
    request.setId(302L);
    request.setOrderState(2);
    when(orderApplicationService.updateOrderStatus(9L, false, false, request))
        .thenReturn(cn.edu.tju.core.model.HttpResult.success(request));

    var result = orderController.updateOrderStatus(request);

    assertTrue(result.getSuccess());
    verify(orderApplicationService).updateOrderStatus(9L, false, false, request);
  }

  @Test
  void updateOrderStatus_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    Order request = new Order();
    request.setId(300L);
    request.setOrderState(3);

    var result = orderController.updateOrderStatus(request);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).updateOrderStatus(9L, false, false, request);
  }

  @Test
  void cancelOrder_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = orderController.cancelOrder(400L);

    assertFalse(result.getSuccess());
    verify(orderApplicationService, never()).cancelOrder(9L, 400L);
  }

  @Test
  void cancelOrder_shouldDelegateWhenAuthorized() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Order canceled = new Order();
    canceled.setId(400L);
    canceled.setCustomerId(9L);
    when(orderApplicationService.cancelOrder(9L, 400L))
        .thenReturn(cn.edu.tju.core.model.HttpResult.success(canceled));

    var result = orderController.cancelOrder(400L);

    assertTrue(result.getSuccess());
    verify(orderApplicationService).cancelOrder(9L, 400L);
  }

  @Test
  void addOrders_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = orderController.addOrders(new Order(), "req-add-no-auth");

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(orderApplicationService, never()).addOrder(9L, new Order(), "req-add-no-auth");
  }

  @Test
  void getMerchantOrders_shouldFailWhenNotBusinessUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = orderController.getMerchantOrders();

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
    verify(businessService, never()).getBusinessByOwnerId(me.getId());
  }

  @Test
  void getMerchantOrders_shouldAggregateOrdersAcrossBusinesses() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Business businessA = new Business();
    businessA.setId(100L);
    Business businessB = new Business();
    businessB.setId(101L);
    when(businessService.getBusinessByOwnerId(9L)).thenReturn(java.util.List.of(businessA, businessB));

    Order orderA = new Order();
    orderA.setId(1L);
    Order orderB = new Order();
    orderB.setId(2L);
    when(orderApplicationService.getOrdersByBusinessId(100L)).thenReturn(java.util.List.of(orderA));
    when(orderApplicationService.getOrdersByBusinessId(101L)).thenReturn(java.util.List.of(orderB));

    var result = orderController.getMerchantOrders();

    assertTrue(result.getSuccess());
    assertEquals(2, result.getData().size());
    verify(orderApplicationService).getOrdersByBusinessId(100L);
    verify(orderApplicationService).getOrdersByBusinessId(101L);
  }

  @Test
  void getOrdersByBusinessId_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = orderController.getOrdersByBusinessId(100L);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(businessService, never()).getBusinessById(100L);
  }

  @Test
  void getOrdersByBusinessId_shouldFailWhenBusinessUserIsNotOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Business business = new Business();
    business.setId(100L);
    business.setBusinessOwnerId(10L);
    when(businessService.getBusinessById(100L)).thenReturn(business);

    var result = orderController.getOrdersByBusinessId(100L);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
    verify(orderApplicationService, never()).getOrdersByBusinessId(100L);
  }
}
