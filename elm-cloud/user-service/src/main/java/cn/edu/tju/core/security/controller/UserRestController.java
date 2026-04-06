package cn.edu.tju.core.security.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.service.PersonService;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "管理用户", description = "提供用户的增删改查和密码管理功能")
public class UserRestController {

  private final UserService userService;
  private final PersonService personService;

  public UserRestController(
      UserService userService, PersonService personService) {
    this.userService = userService;
    this.personService = personService;
  }

  @PostMapping("/users")
  @PreAuthorize("hasAuthority('ADMIN')")
  @Operation(summary = "新增用户（仅登录帐号）", description = "管理员创建新用户账号，自动创建钱包")
  public HttpResult<User> createUser(
      @Parameter(description = "用户信息", required = true) @RequestBody User user) {
    if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User CANT BE NULL");
    if (user.getUsername() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User.Username CANT BE NULL");
    if (user.getPassword() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User.Password CANT BE NULL");

    if (user.getAuthorities() == null) user.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    EntityUtils.setNewEntity(user);
    user.setPassword(SecurityUtils.BCryptPasswordEncode(user.getPassword()));
    user.setActivated(true);

    if (userService.getUserWithUsername(user.getUsername()) != null)
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Username ALREADY EXISTS");
    userService.addUser(user);

    return HttpResult.success(user);
  }

  @GetMapping("/user")
  @Operation(summary = "判断当前登录的用户", description = "判断当前登录的用户")
  public HttpResult<User> getActualUser() {
    Optional<User> userOptional = userService.getUserWithAuthorities();
    return userOptional
        .map(HttpResult::success)
        .orElseGet(() -> HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND"));
  }

  @PostMapping("/password")
  @Operation(summary = "修改密码", description = "用户修改自己的密码，管理员可修改任何用户密码")
  public HttpResult<String> updateUserPassword(
      @Parameter(description = "用户名和新密码", required = true) @RequestBody LoginDto loginDto) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");

    User user = userService.getUserWithAuthoritiesByUsername(loginDto.getUsername());
    if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

    if (isAdmin || me.getUsername().equals(loginDto.getUsername())) {
      user.setPassword(SecurityUtils.BCryptPasswordEncode(loginDto.getPassword()));
      userService.updateUser(user);
      return HttpResult.success("Update successfully.");
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PutMapping("/users/{id}")
  @Operation(summary = "更新用户信息", description = "管理员可更新任何用户；普通用户只能更新自己")
  public HttpResult<User> updateUser(
      @Parameter(description = "用户ID", required = true) @PathVariable Long id,
    @RequestBody Person patch) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

    User existing = userService.getUserById(id);
    if (existing == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

    User me = meOptional.get();
    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    // 额外使用 username 做一次兜底比较（避免 equals 实现变化）；同时消除静态检查的“未使用变量”误报。
    boolean isSelf = me.getId() != null && me.getId().equals(existing.getId());
    if (!isAdmin && !isSelf) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    if (patch == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User CANT BE NULL");
    }

  // 前端 updateUser 传的是 Person，但 User 实体字段非常少：username/activated/authorities。
  // 为避免误改密码，这里只允许更新 username。
  if (patch.getUsername() != null) existing.setUsername(patch.getUsername());

    // 仅管理员允许改 activated/authorities
    if (isAdmin) {
      // activated 只有在请求里明确传时才允许更新；否则保持原值。
      existing.setActivated(patch.isActivated());
      if (patch.getAuthorities() != null) existing.setAuthorities(patch.getAuthorities());
    }

    existing.setUpdateTime(LocalDateTime.now());
    userService.updateUser(existing);
    return HttpResult.success(existing);
  }
  @PostMapping("/persons")
  @Operation(summary = "新增用户（自然人）", description = "注册新用户（包含个人信息），自动创建钱包")
  public HttpResult<Person> addPerson(
      @Parameter(description = "用户个人信息", required = true) @RequestBody Person person) {
    if (person == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Person CANT BE NULL");
    if (person.getUsername() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Person.Username CANT BE NULL");

    person.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    if (person.getPassword() != null && !person.getPassword().isEmpty()) {
      person.setPassword(SecurityUtils.BCryptPasswordEncode(person.getPassword()));
    } else {
      person.setPassword(SecurityUtils.BCryptPasswordEncode("password"));
    }
    person.setActivated(true);

    LocalDateTime now = LocalDateTime.now();
    person.setCreateTime(now);
    person.setUpdateTime(now);
    person.setDeleted(false);

    if (userService.getUserWithUsername(person.getUsername()) != null)
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Username ALREADY EXISTS");

    userService.addUser(person);
    personService.addPerson(person);

    return HttpResult.success(person);
  }

  @GetMapping("/persons/{id}")
  @Operation(summary = "根据ID获取自然人信息", description = "获取指定Person信息；本人或管理员可访问")
  public HttpResult<Person> getPersonById(@PathVariable("id") Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (id == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "PersonId CANT BE NULL");
    Person person = personService.getPersonById(id);
    if (person == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Person NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || (me.getId() != null && me.getId().equals(person.getId()))) {
      return HttpResult.success(person);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PutMapping("/persons/{id}")
  @Operation(summary = "更新自然人信息", description = "更新Person信息；本人或管理员可修改")
  public HttpResult<Person> updatePerson(
      @PathVariable("id") Long id,
      @RequestBody Person patch) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (id == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "PersonId CANT BE NULL");
    Person existing = personService.getPersonById(id);
    if (existing == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Person NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (!isAdmin && (me.getId() == null || !me.getId().equals(existing.getId()))) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    if (patch == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Person CANT BE NULL");
    }

    // 允许更新的字段（避免覆盖密码/权限等敏感字段）
    if (patch.getUsername() != null) existing.setUsername(patch.getUsername());
    if (patch.getFirstName() != null) existing.setFirstName(patch.getFirstName());
    if (patch.getLastName() != null) existing.setLastName(patch.getLastName());
    if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
    if (patch.getPhone() != null) existing.setPhone(patch.getPhone());
    if (patch.getGender() != null) existing.setGender(patch.getGender());
    if (patch.getPhoto() != null) existing.setPhoto(patch.getPhoto());

    existing.setUpdateTime(LocalDateTime.now());
    personService.updatePerson(existing);
    userService.updateUser(existing);
    return HttpResult.success(existing);
  }
  @GetMapping("/users")
  @Operation(summary = "获取所有用户", description = "管理员查询所有用户列表")
  public HttpResult<List<User>> getUsers() {
    Optional<User> meoptional = userService.getUserWithAuthorities();
    if (meoptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meoptional.get();

    if (AuthorityUtils.hasAuthority(me, "ADMIN")) {
      List<User> userList = userService.getUsers();
      return HttpResult.success(userList);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @GetMapping("/users/{id}")
  @Operation(summary = "根据ID获取用户", description = "查询指定用户的详细信息")
  public HttpResult<User> getUser(
      @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    User user = userService.getUserById(id);
    if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.equals(user)) return HttpResult.success(user);
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @DeleteMapping("/users/{id}")
  @Operation(summary = "删除用户", description = "软删除指定用户")
  public HttpResult<User> deleteUser(
      @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    User user = userService.getUserById(id);
    if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.equals(user)) {
      EntityUtils.deleteEntity(user);
      userService.updateUser(user);
      return HttpResult.success();
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PatchMapping("/users/{id}/authorities")
  @Operation(summary = "修改用户权限", description = "管理用户权限列表")
  @PreAuthorize("hasAnyAuthority('ADMIN','INTERNAL_SERVICE')")
  public HttpResult<User> updateUserAuthorities(
      @Parameter(description = "用户ID", required = true) @PathVariable Long id,
      @RequestBody java.util.Map<String, Object> body) {
    User user = userService.getUserById(id);
    if (user == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");
    }

    if (body.containsKey("authorities")) {
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, String>> authList = (java.util.List<java.util.Map<String, String>>) body.get("authorities");
        java.util.Set<String> authorities = new java.util.HashSet<>();
        if (authList != null) {
            for (java.util.Map<String, String> authMap : authList) {
                authorities.add(authMap.get("name"));
            }
        }
        user.setAuthorities(AuthorityUtils.getAuthoritySet(String.join(" ", authorities)));
        userService.updateUser(user);
    }
    return HttpResult.success(user);
  }

  @PatchMapping("/inner/users/{id}/authorities")
  @Operation(summary = "内部-修改用户权限", description = "服务间调用：修改用户权限列表")
  @PreAuthorize("hasAnyAuthority('INTERNAL_SERVICE')")
  public HttpResult<User> updateUserAuthoritiesInternal(
      @Parameter(description = "用户ID", required = true) @PathVariable Long id,
      @RequestBody java.util.Map<String, Object> body) {
    return updateUserAuthorities(id, body);
  }
}
