package cn.edu.tju.core.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.AuthorityRepository;
import cn.edu.tju.core.security.repository.PersonRepository;
import cn.edu.tju.core.security.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserDomainServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PersonRepository personRepository;
  @Mock private AuthorityRepository authorityRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AccountProvisioningClient accountProvisioningClient;

  @InjectMocks private UserDomainService userDomainService;

  @Test
  void createUserNormalizesUsernameAndEnsuresWallet() {
    User user = new User();
    user.setUsername("Alice");
    user.setPassword("123456");
    user.setActivated(false);

    when(userRepository.getUserByUsername("alice")).thenReturn(Optional.empty());
    when(authorityRepository.findById("USER")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("123456")).thenReturn("encoded-123456");
    when(authorityRepository.save(any(Authority.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User saved = invocation.getArgument(0);
              saved.setId(99L);
              return saved;
            });

    User saved = userDomainService.createUser(user);

    assertEquals("alice", saved.getUsername());
    assertEquals("encoded-123456", saved.getPassword());
    assertTrue(saved.isActivated());
    assertFalse(Boolean.TRUE.equals(saved.getDeleted()));
    assertNotNull(saved.getCreateTime());
    assertNotNull(saved.getUpdateTime());
    assertEquals(Set.of(newAuthority("USER")), saved.getAuthorities());
    verify(accountProvisioningClient).ensureWallet(99L);
  }

  @Test
  void createUserRejectsDuplicateUsername() {
    User existing = new User();
    existing.setId(1L);
    existing.setUsername("alice");

    User user = new User();
    user.setUsername("Alice");

    when(userRepository.getUserByUsername("alice")).thenReturn(Optional.of(existing));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> userDomainService.createUser(user));

    assertEquals("Username ALREADY EXISTS", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
    verify(accountProvisioningClient, never()).ensureWallet(any());
  }

  @Test
  void updateUserAppliesNormalizedUsernameEncodedPasswordAndAuthorities() {
    User existing = new User();
    existing.setId(5L);
    existing.setUsername("oldname");
    existing.setPassword("old-password");
    existing.setActivated(false);

    User patch = new User();
    patch.setId(5L);
    patch.setUsername("Bob");
    patch.setPassword("new-password");
    patch.setActivated(true);
    patch.setAuthorities(Set.of(newAuthority("ADMIN")));

    when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
    when(userRepository.getUserByUsername("bob")).thenReturn(Optional.empty());
    when(authorityRepository.findById("ADMIN")).thenReturn(Optional.of(newAuthority("ADMIN")));
    when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userDomainService.updateUser(patch);

    assertEquals("bob", updated.getUsername());
    assertEquals("encoded-new-password", updated.getPassword());
    assertTrue(updated.isActivated());
    assertEquals(Set.of(newAuthority("ADMIN")), updated.getAuthorities());
    assertNotNull(updated.getUpdateTime());
  }

  @Test
  void getUsersFiltersDeletedRecords() {
    User active = new User();
    active.setId(1L);
    active.setUsername("active");
    active.setDeleted(false);

    User deleted = new User();
    deleted.setId(2L);
    deleted.setUsername("deleted");
    deleted.setDeleted(true);

    when(userRepository.findAll()).thenReturn(List.of(active, deleted));

    List<User> users = userDomainService.getUsers();

    assertEquals(1, users.size());
    assertEquals("active", users.get(0).getUsername());
  }

  private static Authority newAuthority(String name) {
    Authority authority = new Authority();
    authority.setName(name);
    return authority;
  }
}