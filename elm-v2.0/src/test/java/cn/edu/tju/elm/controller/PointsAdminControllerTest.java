package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.PointsRule;
import cn.edu.tju.elm.service.PointsService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointsAdminControllerTest {

  @Mock private UserService userService;
  @Mock private PointsService pointsService;

  @InjectMocks private PointsAdminController pointsAdminController;

  @Test
  void createPointsRule_shouldFailWhenUserNotLoggedIn() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = pointsAdminController.createPointsRule(new PointsRule());

    assertFalse(result.getSuccess());
    assertEquals("用户未登录", result.getMessage());
    verify(pointsService, never()).createPointsRule(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createPointsRule_shouldFailWhenNotAdmin() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = pointsAdminController.createPointsRule(new PointsRule());

    assertFalse(result.getSuccess());
    assertEquals("需要管理员权限", result.getMessage());
  }

  @Test
  void createPointsRule_shouldSetCreatorAndUpdater() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    PointsRule rule = new PointsRule();
    rule.setChannelType("ORDER");
    PointsRule created = new PointsRule();
    created.setId(101L);
    created.setChannelType("ORDER");
    created.setCreator(9L);
    created.setUpdater(9L);
    when(pointsService.createPointsRule(rule)).thenReturn(created);

    var result = pointsAdminController.createPointsRule(rule);

    assertTrue(result.getSuccess());
    assertEquals(9L, rule.getCreator());
    assertEquals(9L, rule.getUpdater());
    assertEquals(101L, result.getData().getId());
    assertEquals("ORDER", result.getData().getChannelType());
  }

  @Test
  void getAllPointsRules_shouldReturnRuleVos() {
    PointsRule rule = new PointsRule();
    rule.setId(101L);
    rule.setChannelType("ORDER");
    when(pointsService.getAllPointsRules()).thenReturn(List.of(rule));

    var result = pointsAdminController.getAllPointsRules();

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
    assertEquals(101L, result.getData().getFirst().getId());
  }

  @Test
  void updatePointsRule_shouldMapPointsExceptionToNotFound() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    PointsRule rule = new PointsRule();
    when(pointsService.updatePointsRule(101L, rule))
        .thenThrow(new PointsException(PointsException.RULE_NOT_FOUND));

    var result = pointsAdminController.updatePointsRule(101L, rule);

    assertFalse(result.getSuccess());
    assertEquals(PointsException.RULE_NOT_FOUND, result.getMessage());
    assertEquals(9L, rule.getUpdater());
  }

  @Test
  void deletePointsRule_shouldDeleteWhenAdmin() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    var result = pointsAdminController.deletePointsRule(101L);

    assertTrue(result.getSuccess());
    assertEquals("删除成功", result.getData());
    verify(pointsService).deletePointsRule(101L);
  }
}