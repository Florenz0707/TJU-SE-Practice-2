package cn.edu.tju.core.security.controller;

import cn.edu.tju.core.model.*;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.security.controller.dto.LoginDto;
import cn.edu.tju.core.security.service.PersonService;
import cn.edu.tju.elm.utils.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import cn.edu.tju.core.security.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理用户", description = "提供用户的增删改查操作")
public class UserRestController {

    private final UserService userService;
    private final PersonService personService;

    public UserRestController(UserService userService, PersonService personService) {
        this.userService = userService;
        this.personService = personService;
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "新增用户（仅登录帐号）", description = "创建一个新的用户（仅登录帐号）")
    public User createUser(@RequestBody User user) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return null;
        User me = meOptional.get();

        if (user == null)
            return null;

        if (user.getUsername() == null)
            return null;
        if (user.getPassword() == null)
            return null;

        Utils.setNewEntity(user, me);
        user.setPassword(SecurityUtils.BCryptPasswordEncode(user.getPassword()));
        user.setActivated(true);

        if (userService.getUserWithUsername(user.getUsername()) != null)
            return null;
        userService.addUser(user);
        return user;
    }

    @GetMapping("/user")
    @Operation(summary = "判断当前登录的用户", description = "判断当前登录的用户")
    public User getActualUser() {
        Optional<User> userOptional = userService.getUserWithAuthorities();
        return userOptional.orElse(null);
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码", description = "已登录的用户只可以修改自己的密码，Admin可以修改任何人的密码")
    public String updateUserPassword(@RequestBody LoginDto loginDto) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return "AUTHORITY NOT FOUND";
        User me = meOptional.get();

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");

        User user = userService.getUserWithAuthoritiesByUsername(loginDto.getUsername());
        if (user == null)
            return "User NOT FOUND";

        if (isAdmin || me.getUsername().equals(loginDto.getUsername())) {
            user.setPassword(SecurityUtils.BCryptPasswordEncode(loginDto.getPassword()));
            userService.updateUser(user);
            return "Update successfully.";
        }
        return "AUTHORITY LACKED";
    }

    @PostMapping("/persons")
    @Operation(summary = "新增用户（自然人）", description = "创建一个新的用户（自然人）")
    public Person addPerson(@RequestBody Person person) {
        if (person == null)
            return null;
        if (person.getUsername() == null)
            return null;

        if (person.getAuthorities() == null || person.getAuthorities().isEmpty())
            person.setAuthorities(Utils.getAuthoritySet("USER"));
        person.setPassword(SecurityUtils.BCryptPasswordEncode("password"));
        person.setActivated(true);

        LocalDateTime now = LocalDateTime.now();
        person.setCreateTime(now);
        person.setUpdateTime(now);
        person.setCreator(0L);
        person.setUpdater(0L);
        person.setDeleted(false);

        if (userService.getUserWithUsername(person.getUsername()) != null)
            return null;

        userService.addUser(person);
        personService.addPerson(person);
        return person;
    }

    @GetMapping("/users")
    public HttpResult<List<User>> getUsers() {
        Optional<User> meoptional = userService.getUserWithAuthorities();
        if (meoptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meoptional.get();

        if (Utils.hasAuthority(me, "ADMIN")) {
            List<User> userList = userService.getUsers();
            return HttpResult.success(userList);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/users/{id}")
    public HttpResult<User> getUser(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        User user = userService.getUserById(id);
        if (user == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(user))
            return HttpResult.success(user);
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/users/{id}")
    public HttpResult<User> deleteUser(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        User user = userService.getUserById(id);
        if (user == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(user)) {
            Utils.deleteEntity(user, me);
            userService.updateUser(user);
            return HttpResult.success();
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}
