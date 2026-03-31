package cn.edu.tju.core.security.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.service.UserDomainService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserRestController {

  private final UserDomainService userDomainService;

  public UserRestController(UserDomainService userDomainService) {
    this.userDomainService = userDomainService;
  }

  @PostMapping("/users")
  @PreAuthorize("hasAuthority('ADMIN')")
  public HttpResult<User> createUser(@RequestBody User user) {
    if (user == null || user.getUsername() == null || user.getPassword() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User CANT BE NULL");
    }
    try {
      return HttpResult.success(userDomainService.createUser(user));
    } catch (IllegalArgumentException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/user")
  public HttpResult<User> getActualUser() {
    Optional<User> userOptional = userDomainService.getCurrentUserWithAuthorities();
    return userOptional
        .map(HttpResult::success)
        .orElseGet(() -> HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND"));
  }

  @PostMapping("/password")
  public HttpResult<String> updateUserPassword(@RequestBody LoginDto loginDto) {
    Optional<User> meOptional = userDomainService.getCurrentUserWithAuthorities();
    if (meOptional.isEmpty()) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    User me = meOptional.get();
    User user = userDomainService.getUserWithAuthoritiesByUsername(loginDto.getUsername());
    if (user == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");
    }
    boolean isAdmin = me.getAuthorities().stream().anyMatch(authority -> "ADMIN".equals(authority.getName()));
    if (isAdmin || me.getUsername().equalsIgnoreCase(loginDto.getUsername())) {
      user.setPassword(SecurityUtils.bCryptPasswordEncode(loginDto.getPassword()));
      userDomainService.updateUser(user);
      return HttpResult.success("Update successfully.");
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PostMapping("/persons")
  public HttpResult<Person> addPerson(@RequestBody Person person) {
    if (person == null || person.getUsername() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Person CANT BE NULL");
    }
    person.setPassword(SecurityUtils.bCryptPasswordEncode("password"));
    person.setActivated(true);
    LocalDateTime now = LocalDateTime.now();
    person.setCreateTime(now);
    person.setUpdateTime(now);
    person.setDeleted(false);
    try {
      return HttpResult.success(userDomainService.createPerson(person));
    } catch (IllegalArgumentException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/users")
  public HttpResult<List<User>> getUsers() {
    return HttpResult.success(userDomainService.getUsers());
  }

  @GetMapping("/users/{id}")
  public HttpResult<User> getUser(@PathVariable Long id) {
    Optional<User> meOptional = userDomainService.getCurrentUserWithAuthorities();
    if (meOptional.isEmpty()) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    User me = meOptional.get();
    User user = userDomainService.getUserById(id);
    if (user == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");
    }
    boolean isAdmin = me.getAuthorities().stream().anyMatch(authority -> "ADMIN".equals(authority.getName()));
    if (isAdmin || me.getId().equals(user.getId())) {
      return HttpResult.success(user);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @DeleteMapping("/users/{id}")
  public HttpResult<User> deleteUser(@PathVariable Long id) {
    Optional<User> meOptional = userDomainService.getCurrentUserWithAuthorities();
    if (meOptional.isEmpty()) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    }
    User me = meOptional.get();
    User user = userDomainService.getUserById(id);
    if (user == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");
    }
    boolean isAdmin = me.getAuthorities().stream().anyMatch(authority -> "ADMIN".equals(authority.getName()));
    if (isAdmin || me.getId().equals(user.getId())) {
      user.setDeleted(true);
      userDomainService.updateUser(user);
      return HttpResult.success();
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }
}