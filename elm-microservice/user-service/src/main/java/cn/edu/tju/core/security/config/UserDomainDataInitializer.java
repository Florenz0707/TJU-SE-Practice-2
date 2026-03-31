package cn.edu.tju.core.security.config;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.AuthorityRepository;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.core.security.service.AccountProvisioningClient;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserDomainDataInitializer implements CommandLineRunner {

  private static final String DEMO_PASSWORD = "123456";

  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;
  private final PasswordEncoder passwordEncoder;
  private final AccountProvisioningClient accountProvisioningClient;

  public UserDomainDataInitializer(
      UserRepository userRepository,
      AuthorityRepository authorityRepository,
      PasswordEncoder passwordEncoder,
      AccountProvisioningClient accountProvisioningClient) {
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.passwordEncoder = passwordEncoder;
    this.accountProvisioningClient = accountProvisioningClient;
  }

  @Override
  @Transactional
  public void run(String... args) {
    Authority adminAuthority = ensureAuthority("ADMIN");
    Authority userAuthority = ensureAuthority("USER");
    Authority businessAuthority = ensureAuthority("BUSINESS");

    ensureUser("admin", DEMO_PASSWORD, Set.of(adminAuthority, businessAuthority, userAuthority));
    ensureUser("user", DEMO_PASSWORD, Set.of(userAuthority));
    ensureUser("business", DEMO_PASSWORD, Set.of(businessAuthority, userAuthority));
  }

  private Authority ensureAuthority(String name) {
    return authorityRepository
        .findById(name)
        .orElseGet(
            () -> {
              Authority authority = new Authority();
              authority.setName(name);
              return authorityRepository.save(authority);
            });
  }

  private void ensureUser(String username, String rawPassword, Set<Authority> authorities) {
    User user =
        userRepository
            .getUserWithAuthoritiesByUsername(username)
            .orElseGet(
                () -> {
                  User created = new User();
                  created.setUsername(username);
                  created.setPassword(passwordEncoder.encode(rawPassword));
                  created.setActivated(true);
                  created.setDeleted(false);
                  created.setCreateTime(LocalDateTime.now());
                  created.setUpdateTime(LocalDateTime.now());
                  created.setAuthorities(authorities);
                  return userRepository.save(created);
                });

    boolean changed = false;
    if (!user.isActivated()) {
      user.setActivated(true);
      changed = true;
    }
    if (Boolean.TRUE.equals(user.getDeleted())) {
      user.setDeleted(false);
      changed = true;
    }
    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
      user.setPassword(passwordEncoder.encode(rawPassword));
      changed = true;
    }
    if (!user.getAuthorities().containsAll(authorities) || user.getAuthorities().size() != authorities.size()) {
      user.setAuthorities(authorities);
      changed = true;
    }
    if (changed) {
      user.setUpdateTime(LocalDateTime.now());
      userRepository.save(user);
    }

    try {
      accountProvisioningClient.ensureWallet(user.getId());
    } catch (Exception e) {
      System.err.println("Skip wallet bootstrap for user " + username + ": " + e.getMessage());
    }
  }
}