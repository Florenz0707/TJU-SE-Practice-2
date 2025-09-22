package cn.edu.tju.core.security.rest;

import cn.edu.tju.core.model.*;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.security.UserModelDetailsService;
import cn.edu.tju.core.security.repository.PersonRepository;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.core.security.rest.dto.LoginDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
    private final PersonRepository personRepository;
    private final UserModelDetailsService userModelDetailsService;
    private final UserRepository userRepository;

    public UserRestController(UserService userService, PersonRepository personRepository, UserModelDetailsService userModelDetailsService, UserRepository userRepository) {
        this.userService = userService;
        this.personRepository = personRepository;
        this.userModelDetailsService = userModelDetailsService;
        this.userRepository = userRepository;
    }

    @PostMapping("/users")
    @Operation(summary = "新增用户（仅登录帐号）", description = "创建一个新的用户（仅登录帐号）")
    public ResponseEntity<User> createUser(@RequestBody User newUser) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return null;
        User me = meOptional.get();

        LocalDateTime now = LocalDateTime.now();
        newUser.setCreator(me.getId());
        newUser.setCreateTime(now);
        newUser.setUpdater(me.getId());
        newUser.setUpdateTime(now);
        newUser.setDeleted(false);
        newUser.setPassword(SecurityUtils.BCryptPasswordEncode("password"));
        newUser.setActivated(true);

        User user = userService.addUser(newUser);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/user")
    @Operation(summary = "判断当前登录的用户", description = "判断当前登录的用户")
    public ResponseEntity<User> getActualUser() {
        Optional<User> userOptional = userService.getUserWithAuthorities();
        return userOptional.map(ResponseEntity::ok).orElse(null);
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码", description = "已登录的用户只可以修改自己的密码，Admin可以修改任何人的密码")
    public ResponseEntity<String> updateUserPassword(@RequestBody LoginDto loginDto) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return ResponseEntity.unprocessableEntity().body("Failed to update the password.");

        User me = meOptional.get();
        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }

        Optional<User> updateUserOptional = userRepository.findOneWithAuthoritiesByUsername(loginDto.getUsername());
        if (updateUserOptional.isEmpty())
            return ResponseEntity.unprocessableEntity().body("Failed to update the password.");

        User updateUser = updateUserOptional.get();
        if (isAdmin || me.getUsername().equals(loginDto.getUsername())) {
            updateUser.setPassword(SecurityUtils.BCryptPasswordEncode(loginDto.getPassword()));
            userService.updateUser(updateUser);
            return ResponseEntity.ok().body("Update the password successfully.");
        }
        return ResponseEntity.unprocessableEntity().body("Failed to update the password.");
    }

    @PostMapping("/persons")
    @Operation(summary = "新增用户（自然人）", description = "创建一个新的用户（自然人）")
    public Person addPerson(@RequestBody Person person) {
        // TODO: 去除authority检查，允许任意新建用户
        // TODO: 同时新设System角色于初始User表中，作为默认Admin
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return null;
        User me = meOptional.get();

        LocalDateTime now = LocalDateTime.now();
        person.setCreator(me.getId());
        person.setCreateTime(now);
        person.setUpdater(me.getId());
        person.setUpdateTime(now);
        person.setDeleted(false);
        person.setPassword(SecurityUtils.BCryptPasswordEncode("password"));
        person.setActivated(true);
        return personRepository.save(person);
    }

    @GetMapping("/users")
    public HttpResult<List<User>> getUsers() {
        Optional<User> meoptional = userService.getUserWithAuthorities();
        if (meoptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meoptional.get();

        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                return HttpResult.success(userService.getUsers());
            }
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/users/{id}")
    public HttpResult<User> getUser(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        User user = userService.getUserById(id);
        if (user == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }

        if (isAdmin || me.equals(user))
            return HttpResult.success(user);
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PutMapping("/users/{id}")
    public HttpResult<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        User oldUser = userService.getUserById(id);
        if (oldUser == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "User NOT FOUND");

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }
        if (isAdmin || me.equals(oldUser)) {
            user.setId(oldUser.getId());
            user.setUsername(oldUser.getUsername());
            user.setPassword(oldUser.getPassword());
            user.setActivated(oldUser.isActivated());
            user.setAuthorities(oldUser.getAuthorities());

            LocalDateTime now = LocalDateTime.now();
            user.setCreateTime(oldUser.getCreateTime());
            user.setUpdateTime(now);
            user.setCreator(oldUser.getCreator());
            user.setUpdater(me.getId());
            user.setDeleted(false);

            userService.updateUser(user);
            return HttpResult.success(user);
        }
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

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }
        if (isAdmin || me.equals(user)) {
            LocalDateTime now = LocalDateTime.now();
            user.setUpdateTime(now);
            user.setUpdater(me.getId());
            user.setDeleted(true);
            userService.updateUser(user);
            return HttpResult.success(user);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    public UserModelDetailsService getUserModelDetailsService() {
        return userModelDetailsService;
    }
}
