package cn.edu.tju.core.security.service;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.security.repository.AuthorityRepository;
import cn.edu.tju.core.security.repository.PersonRepository;
import cn.edu.tju.core.security.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserDomainService {

  private final UserRepository userRepository;
  private final PersonRepository personRepository;
  private final AuthorityRepository authorityRepository;
  private final PasswordEncoder passwordEncoder;
  private final AccountProvisioningClient accountProvisioningClient;

  public UserDomainService(
      UserRepository userRepository,
      PersonRepository personRepository,
      AuthorityRepository authorityRepository,
      PasswordEncoder passwordEncoder,
      AccountProvisioningClient accountProvisioningClient) {
    this.userRepository = userRepository;
    this.personRepository = personRepository;
    this.authorityRepository = authorityRepository;
    this.passwordEncoder = passwordEncoder;
    this.accountProvisioningClient = accountProvisioningClient;
  }

  @Transactional(readOnly = true)
  public Optional<User> getCurrentUserWithAuthorities() {
    return SecurityUtils.getCurrentUsername().flatMap(this::findByUsernameWithAuthoritiesOptional);
  }

  @Transactional(readOnly = true)
  public List<User> getUsers() {
    return userRepository.findAll().stream().filter(user -> !Boolean.TRUE.equals(user.getDeleted())).toList();
  }

  @Transactional(readOnly = true)
  public User getUserById(Long id) {
    return userRepository.findById(id).filter(user -> !Boolean.TRUE.equals(user.getDeleted())).orElse(null);
  }

  @Transactional(readOnly = true)
  public User getUserWithAuthoritiesByUsername(String username) {
    return findByUsernameWithAuthoritiesOptional(username).orElse(null);
  }

  @Transactional(readOnly = true)
  public User getUserByUsername(String username) {
    if (username == null) {
      return null;
    }
    return userRepository
        .getUserByUsername(normalizeUsername(username))
        .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
        .orElse(null);
  }

  public User createUser(User user) {
    String normalizedUsername = normalizeUsername(user.getUsername());
    if (userRepository.getUserByUsername(normalizedUsername).isPresent()) {
      throw new IllegalArgumentException("Username ALREADY EXISTS");
    }

    user.setUsername(normalizedUsername);
    user.setAuthorities(resolveAuthorities(user.getAuthorities(), "USER"));
    user.setPassword(encodePasswordIfNeeded(user.getPassword()));
    if (!user.isActivated()) {
      user.setActivated(true);
    }
    setNewEntityIfAbsent(user);
    User saved = userRepository.save(user);
    accountProvisioningClient.ensureWallet(saved.getId());
    return saved;
  }

  public Person createPerson(Person person) {
    String normalizedUsername = normalizeUsername(person.getUsername());
    if (userRepository.getUserByUsername(normalizedUsername).isPresent()) {
      throw new IllegalArgumentException("Username ALREADY EXISTS");
    }

    person.setUsername(normalizedUsername);
    person.setAuthorities(resolveAuthorities(person.getAuthorities(), "USER"));
    person.setPassword(encodePasswordIfNeeded(person.getPassword()));
    person.setActivated(true);
    setNewEntityIfAbsent(person);
    Person saved = personRepository.save(person);
    accountProvisioningClient.ensureWallet(saved.getId());
    return saved;
  }

  public User updateUser(User user) {
    User existing = userRepository.findById(user.getId()).orElseThrow();

    if (user.getUsername() != null) {
      String normalizedUsername = normalizeUsername(user.getUsername());
      User duplicate = getUserByUsername(normalizedUsername);
      if (duplicate != null && !duplicate.getId().equals(existing.getId())) {
        throw new IllegalArgumentException("Username ALREADY EXISTS");
      }
      existing.setUsername(normalizedUsername);
    }
    if (user.getPassword() != null && !user.getPassword().isBlank()) {
      existing.setPassword(encodePasswordIfNeeded(user.getPassword()));
    }
    if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
      existing.setAuthorities(resolveAuthorities(user.getAuthorities(), "USER"));
    }
    existing.setActivated(user.isActivated());
    if (user.getDeleted() != null) {
      existing.setDeleted(user.getDeleted());
    }
    existing.setUpdateTime(LocalDateTime.now());
    return userRepository.save(existing);
  }

  private Optional<User> findByUsernameWithAuthoritiesOptional(String username) {
    if (username == null) {
      return Optional.empty();
    }
    return userRepository
        .getUserWithAuthoritiesByUsername(normalizeUsername(username))
        .filter(user -> !Boolean.TRUE.equals(user.getDeleted()));
  }

  private Set<Authority> resolveAuthorities(Set<Authority> requestedAuthorities, String fallback) {
    Set<String> names = new HashSet<>();
    if (requestedAuthorities != null) {
      for (Authority authority : requestedAuthorities) {
        if (authority != null && authority.getName() != null) {
          names.add(authority.getName().toUpperCase(Locale.ENGLISH));
        }
      }
    }
    if (names.isEmpty()) {
      for (String value : fallback.split(" ")) {
        names.add(value.toUpperCase(Locale.ENGLISH));
      }
    }

    return names.stream()
        .map(this::ensureAuthority)
        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
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

  private void setNewEntityIfAbsent(User user) {
    LocalDateTime now = LocalDateTime.now();
    if (user.getCreateTime() == null) {
      user.setCreateTime(now);
    }
    user.setUpdateTime(now);
    if (user.getDeleted() == null) {
      user.setDeleted(false);
    }
  }

  private String encodePasswordIfNeeded(String rawOrEncodedPassword) {
    if (rawOrEncodedPassword == null) {
      return null;
    }
    if (rawOrEncodedPassword.startsWith("$2a$")
        || rawOrEncodedPassword.startsWith("$2b$")
        || rawOrEncodedPassword.startsWith("$2y$")) {
      return rawOrEncodedPassword;
    }
    return passwordEncoder.encode(rawOrEncodedPassword);
  }

  private String normalizeUsername(String username) {
    return username == null ? null : username.toLowerCase(Locale.ENGLISH);
  }
}