package cn.edu.tju.core.security.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.service.UserDomainService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRestControllerTest {

  @Mock private UserDomainService userDomainService;

  @InjectMocks private UserRestController userRestController;

  @Test
  void addPersonInitializesDefaultsBeforeDelegating() {
    Person input = new Person();
    input.setUsername("alice");

    when(userDomainService.createPerson(any(Person.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    HttpResult<Person> result = userRestController.addPerson(input);

    assertTrue(result.getSuccess());
    Person created = result.getData();
    assertNotNull(created);
    assertTrue(created.isActivated());
    assertFalse(Boolean.TRUE.equals(created.getDeleted()));
    assertNotNull(created.getCreateTime());
    assertNotNull(created.getUpdateTime());
    assertNotNull(created.getPassword());
    assertTrue(created.getPassword().startsWith("$2"));
  }

  @Test
  void updateUserPasswordRejectsNonOwnerNonAdmin() {
    User me = user(1L, "alice", "USER");
    User target = user(2L, "bob", "USER");
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername("bob");
    loginDto.setPassword("new-password");

    when(userDomainService.getCurrentUserWithAuthorities()).thenReturn(Optional.of(me));
    when(userDomainService.getUserWithAuthoritiesByUsername("bob")).thenReturn(target);

    HttpResult<String> result = userRestController.updateUserPassword(loginDto);

    assertFalse(result.getSuccess());
    assertEquals("FORBIDDEN", result.getCode());
    assertEquals("AUTHORITY LACKED", result.getMessage());
  }

  @Test
  void getUserReturnsForbiddenForDifferentNonAdminUser() {
    User me = user(1L, "alice", "USER");
    User target = user(2L, "bob", "USER");

    when(userDomainService.getCurrentUserWithAuthorities()).thenReturn(Optional.of(me));
    when(userDomainService.getUserById(2L)).thenReturn(target);

    HttpResult<User> result = userRestController.getUser(2L);

    assertFalse(result.getSuccess());
    assertEquals("FORBIDDEN", result.getCode());
    assertEquals("AUTHORITY LACKED", result.getMessage());
  }

  @Test
  void deleteUserAsAdminMarksDeletedAndPersists() {
    User admin = user(10L, "admin", "ADMIN");
    User target = user(2L, "bob", "USER");

    when(userDomainService.getCurrentUserWithAuthorities()).thenReturn(Optional.of(admin));
    when(userDomainService.getUserById(2L)).thenReturn(target);

    HttpResult<User> result = userRestController.deleteUser(2L);

    assertTrue(result.getSuccess());
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userDomainService).updateUser(captor.capture());
    assertTrue(Boolean.TRUE.equals(captor.getValue().getDeleted()));
  }

  private static User user(Long id, String username, String authorityName) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setAuthorities(Set.of(authority(authorityName)));
    return user;
  }

  private static Authority authority(String name) {
    Authority authority = new Authority();
    authority.setName(name);
    return authority;
  }
}