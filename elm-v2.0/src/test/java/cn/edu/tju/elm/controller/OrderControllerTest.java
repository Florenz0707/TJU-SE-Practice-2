package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
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
}
