package cn.edu.tju.core.security.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserRestControllerTest {

  @Mock private UserService userService;

  @InjectMocks private UserRestController userRestController;

  @Test
  void createUser_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = userRestController.createUser(new User());

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(userService, never()).addUser(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createUser_shouldFailWhenUsernameAlreadyExists() {
    User me = new User();
    me.setId(1L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User request = new User();
    request.setUsername("existing");
    request.setPassword("123456");
    when(userService.getUserWithUsername("existing")).thenReturn(new User());

    var result = userRestController.createUser(request);

    assertFalse(result.getSuccess());
    assertEquals("Username ALREADY EXISTS", result.getMessage());
    verify(userService, never()).addUser(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createUser_shouldSetDefaultsAndEncodePassword() {
    User me = new User();
    me.setId(1L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User request = new User();
    request.setUsername("alice");
    request.setPassword("123456");
    request.setAuthorities(null);
    when(userService.getUserWithUsername("alice")).thenReturn(null);
    when(userService.addUser(request)).thenReturn(request);

    var result = userRestController.createUser(request);

    assertTrue(result.getSuccess());
    assertSame(request, result.getData());
    assertTrue(request.isActivated());
    assertFalse(Boolean.TRUE.equals(request.getDeleted()));
    assertNotNull(request.getCreateTime());
    assertNotNull(request.getUpdateTime());
    assertTrue(AuthorityUtils.hasAuthority(request, "USER"));
    assertNotEquals("123456", request.getPassword());
    assertTrue(new BCryptPasswordEncoder().matches("123456", request.getPassword()));
  }

  @Test
  void updateUserPassword_shouldFailWhenTargetUserMissing() {
    User me = new User();
    me.setId(9L);
    me.setUsername("me");
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("ghost");
    loginDto.setPassword("new-password");
    when(userService.getUserWithAuthoritiesByUsername("ghost")).thenReturn(null);

    var result = userRestController.updateUserPassword(loginDto);

    assertFalse(result.getSuccess());
    assertEquals("User NOT FOUND", result.getMessage());
  }

  @Test
  void updateUserPassword_shouldAllowSelfUpdate() {
    User me = new User();
    me.setId(9L);
    me.setUsername("me");
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User target = new User();
    target.setId(9L);
    target.setUsername("me");
    target.setPassword("old-password");
    when(userService.getUserWithAuthoritiesByUsername("me")).thenReturn(target);
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("me");
    loginDto.setPassword("new-password");

    var result = userRestController.updateUserPassword(loginDto);

    assertTrue(result.getSuccess());
    assertEquals("Update successfully.", result.getData());
    assertTrue(new BCryptPasswordEncoder().matches("new-password", target.getPassword()));
    verify(userService).updateUser(target);
  }

  @Test
  void updateUserPassword_shouldRejectOtherUserForNonAdmin() {
    User me = new User();
    me.setId(9L);
    me.setUsername("me");
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User target = new User();
    target.setId(10L);
    target.setUsername("other");
    when(userService.getUserWithAuthoritiesByUsername("other")).thenReturn(target);
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("other");
    loginDto.setPassword("new-password");

    var result = userRestController.updateUserPassword(loginDto);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
    verify(userService, never()).updateUser(target);
  }

  @Test
  void addPerson_shouldFailWhenUsernameAlreadyExists() {
    Person request = new Person();
    request.setUsername("existing");
    when(userService.getUserWithUsername("existing")).thenReturn(new User());

    var result = userRestController.addPerson(request);

    assertFalse(result.getSuccess());
    assertEquals("Username ALREADY EXISTS", result.getMessage());
    verify(userService, never()).addPerson(request);
  }

  @Test
  void addPerson_shouldSetDefaultsAndEncodeDefaultPassword() {
    Person request = new Person();
    request.setUsername("new-person");
    when(userService.getUserWithUsername("new-person")).thenReturn(null);
    when(userService.addPerson(request)).thenReturn(request);

    var result = userRestController.addPerson(request);

    assertTrue(result.getSuccess());
    assertSame(request, result.getData());
    assertTrue(request.isActivated());
    assertFalse(Boolean.TRUE.equals(request.getDeleted()));
    assertNotNull(request.getCreateTime());
    assertNotNull(request.getUpdateTime());
    assertTrue(AuthorityUtils.hasAuthority(request, "USER"));
    assertTrue(new BCryptPasswordEncoder().matches("password", request.getPassword()));
  }

  @Test
  void getUsers_shouldReturnUsersForAdmin() {
    User me = new User();
    me.setId(1L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User user = new User();
    user.setId(2L);
    when(userService.getUsers()).thenReturn(List.of(user));

    var result = userRestController.getUsers();

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
  }

  @Test
  void getUser_shouldAllowSelf() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User target = new User();
    target.setId(9L);
    when(userService.getUserById(9L)).thenReturn(target);

    var result = userRestController.getUser(9L);

    assertTrue(result.getSuccess());
    assertSame(target, result.getData());
  }

  @Test
  void getUser_shouldRejectOtherUserForNonAdmin() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User target = new User();
    target.setId(10L);
    when(userService.getUserById(10L)).thenReturn(target);

    var result = userRestController.getUser(10L);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
  }

  @Test
  void deleteUser_shouldSoftDeleteForSelf() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    User target = new User();
    target.setId(9L);
    target.setDeleted(false);
    when(userService.getUserById(9L)).thenReturn(target);

    var result = userRestController.deleteUser(9L);

    assertTrue(result.getSuccess());
    assertTrue(Boolean.TRUE.equals(target.getDeleted()));
    assertNotNull(target.getUpdateTime());
    verify(userService).updateUser(target);
  }
}