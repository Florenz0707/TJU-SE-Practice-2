package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.PointsAccount;
import cn.edu.tju.elm.model.BO.PointsRecord;
import cn.edu.tju.elm.service.PointsService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PointsControllerTest {

  @Mock private UserService userService;
  @Mock private PointsService pointsService;

  @InjectMocks private PointsController pointsController;

  @Test
  void getMyPointsAccount_shouldFailWhenUserNotLoggedIn() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = pointsController.getMyPointsAccount();

    assertFalse(result.getSuccess());
    assertEquals("用户未登录", result.getMessage());
    verify(pointsService, never()).getPointsAccount(9L);
  }

  @Test
  void getMyPointsAccount_shouldFailWhenAccountMissing() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(pointsService.getPointsAccount(9L)).thenReturn(null);

    var result = pointsController.getMyPointsAccount();

    assertFalse(result.getSuccess());
    assertEquals(PointsException.ACCOUNT_NOT_FOUND, result.getMessage());
  }

  @Test
  void getMyPointsAccount_shouldReturnAccountWhenExists() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    PointsAccount account = new PointsAccount();
    account.setUserId(9L);
    account.setTotalPoints(200);
    account.setFrozenPoints(50);
    when(pointsService.getPointsAccount(9L)).thenReturn(account);

    var result = pointsController.getMyPointsAccount();

    assertTrue(result.getSuccess());
    assertEquals(200, result.getData().getTotalPoints());
    assertEquals(50, result.getData().getFrozenPoints());
  }

  @Test
  void getMyPointsAccount_shouldReturnFailureWhenPointsExceptionThrown() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(pointsService.getPointsAccount(9L))
        .thenThrow(new PointsException(PointsException.ACCOUNT_NOT_FOUND));

    var result = pointsController.getMyPointsAccount();

    assertFalse(result.getSuccess());
    assertEquals(PointsException.ACCOUNT_NOT_FOUND, result.getMessage());
  }

  @Test
  void getMyPointsRecords_shouldFailWhenUserNotLoggedIn() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = pointsController.getMyPointsRecords(1, 10, null);

    assertFalse(result.getSuccess());
    assertEquals("用户未登录", result.getMessage());
    verify(pointsService, never()).getPointsRecords(9L, 1, 10, null);
  }

  @Test
  void getMyPointsRecords_shouldReturnPagedRecords() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    PointsRecord record = new PointsRecord();
    record.setUserId(9L);
    record.setType("ORDER_REWARD");
    record.setPoints(30);
    record.setDescription("奖励积分");
    var page = new PageImpl<>(List.of(record), PageRequest.of(0, 10), 1);
    when(pointsService.getPointsRecords(9L, 1, 10, "ORDER_REWARD")).thenReturn(page);

    var result = pointsController.getMyPointsRecords(1, 10, "ORDER_REWARD");

    assertTrue(result.getSuccess());
    assertEquals(1L, result.getData().get("total"));
    assertTrue(result.getData().containsKey("records"));
  }

  @Test
  void getMyPointsRecords_shouldReturnFailureWhenServiceThrows() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(pointsService.getPointsRecords(9L, 1, 10, null))
        .thenThrow(new RuntimeException("points records error"));

    var result = pointsController.getMyPointsRecords(1, 10, null);

    assertFalse(result.getSuccess());
    assertEquals("points records error", result.getMessage());
  }
}