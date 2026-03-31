package cn.edu.tju.core.security.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserDomainService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner")
public class UserInnerController {

  private final UserDomainService userDomainService;

  public UserInnerController(UserDomainService userDomainService) {
    this.userDomainService = userDomainService;
  }

  @GetMapping("/users/{id}")
  public HttpResult<User> getUserById(@PathVariable Long id) {
    User user = userDomainService.getUserById(id);
    return user == null
        ? HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND")
        : HttpResult.success(user);
  }

  @GetMapping("/users/by-username/{username}")
  public HttpResult<User> getUserByUsername(@PathVariable String username) {
    User user = userDomainService.getUserWithAuthoritiesByUsername(username);
    return user == null
        ? HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND")
        : HttpResult.success(user);
  }

  @GetMapping("/users")
  public HttpResult<List<User>> getUsers() {
    return HttpResult.success(userDomainService.getUsers());
  }

  @PostMapping("/users")
  public HttpResult<User> createUser(@RequestBody User user) {
    try {
      return HttpResult.success(userDomainService.createUser(user));
    } catch (IllegalArgumentException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping("/users/{id}")
  public HttpResult<User> updateUser(@PathVariable Long id, @RequestBody User user) {
    user.setId(id);
    try {
      return HttpResult.success(userDomainService.updateUser(user));
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/persons")
  public HttpResult<Person> createPerson(@RequestBody Person person) {
    try {
      return HttpResult.success(userDomainService.createPerson(person));
    } catch (IllegalArgumentException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}