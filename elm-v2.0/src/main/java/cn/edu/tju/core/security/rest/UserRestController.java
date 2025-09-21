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
        User user = userService.getUserWithAuthorities().get();
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreator(user.getId());
        newUser.setCreateTime(now);
        newUser.setUpdater(user.getId());
        newUser.setUpdateTime(now);
        newUser.setDeleted(false);
        newUser.setPassword(SecurityUtils.BCryptPasswordEncode("password"));
        newUser.setActivated(true);
        User user1 = userService.addUser(newUser);
        return ResponseEntity.ok(user1);
    }

    @GetMapping("/user")
    @Operation(summary = "判断当前登录的用户", description = "判断当前登录的用户")
    public ResponseEntity<User> getActualUser() {
        return ResponseEntity.ok(userService.getUserWithAuthorities().get());
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码", description = "已登录的用户只可以修改自己的密码，Admin可以修改任何人的密码")
    public ResponseEntity<String> updateUserPassword(@RequestBody LoginDto loginDto) {
        Optional<User> userOptional = userService.getUserWithAuthorities();
        if (userOptional.isPresent()) {
            User me = userOptional.get();
            boolean isAdmin = false;
            for (Authority authority : me.getAuthorities()) {
                if (authority.getName().equals("ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }

            Optional<User> updateUserOptional = userRepository.findOneWithAuthoritiesByUsername(loginDto.getUsername());
            if (updateUserOptional.isPresent()) {
                User updateUser = updateUserOptional.get();
                if (me.getUsername().equals(loginDto.getUsername()) || isAdmin) {
                    updateUser.setPassword(SecurityUtils.BCryptPasswordEncode(loginDto.getPassword()));
                    userService.updateUser(updateUser);
                    return ResponseEntity.ok().body("Update the password successfully.");
                }
            }
        }
        return ResponseEntity.unprocessableEntity().body("Failed to update the password.");
    }

    @PostMapping("/persons")
    @Operation(summary = "新增用户（自然人）", description = "创建一个新的用户（自然人）")
    public Person addPerson(@RequestBody Person person) {
        Optional<User> userOptional = userService.getUserWithAuthorities();
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            LocalDateTime now = LocalDateTime.now();
            person.setCreator(user.getId());
            person.setCreateTime(now);
            person.setUpdater(user.getId());
            person.setUpdateTime(now);
            person.setDeleted(false);
            person.setPassword(SecurityUtils.BCryptPasswordEncode("password"));
            person.setActivated(true);
            return personRepository.save(person);
        }
        return null;
    }

    @GetMapping("/users")
    public HttpResult<List<User>> getUsers() {
        Optional<User> meoptional = userService.getUserWithAuthorities();
        if(meoptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
        User me = meoptional.get();

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN"))
                isAdmin = true;
        }

        if(isAdmin){
            return HttpResult.success(userService.getUsers());
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/users/{id}")
    public HttpResult<User> getUser(@PathVariable Long id) {
        Optional<User> userOptional = userService.getUserWithAuthorities();
        if(userOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
        User user = userOptional.get();

        boolean isAdmin = false;
        for (Authority authority : user.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        if(isAdmin){
            User user1 = userService.getUserById(id);
            return HttpResult.success(user1);
        }
        return null;
    }

    @PutMapping("/users/{id}")
    public HttpResult<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        Optional<User> userOptional = userService.getUserWithAuthorities();
        if(userOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
        User user1 = userOptional.get();

        User oldUser = userService.getUserById(id);
        if(oldUser == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");

        if(user.getPassword() == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "PASSWORD CANT BE NULL");
        if(user.getUsername() == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "USER CANT BE NULL");
        if(user.getAuthorities() == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "AUTHORITY LACKED");
        User newUser = userService.getUserById(user.getId());
        if(newUser == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");

        boolean isAdmin = false;
        for (Authority authority : user1.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        if(isAdmin || (user1.equals(oldUser) && oldUser.equals(newUser))){
            user.setUpdater(user1.getId());
            LocalDateTime now = LocalDateTime.now();
            user.setUpdateTime(now);
            user.setCreateTime(oldUser.getCreateTime());
            user.setCreator(oldUser.getId());
            userService.updateUser(user);
            return HttpResult.success(user);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/users/{id}")
    public HttpResult<User> deleteUser(@PathVariable Long id) {
        Optional<User> userOptional = userService.getUserWithAuthorities();
        if(userOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
        User user = userOptional.get();

        boolean isAdmin = false;
        for (Authority authority : user.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        if(isAdmin){
            LocalDateTime now = LocalDateTime.now();
            user.setUpdateTime(now);
            user.setUpdater(user.getId());
            user.setDeleted(true);
            userService.updateUser(user);
            return HttpResult.success(userService.getUserById(id));
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    public UserModelDetailsService getUserModelDetailsService() {
        return userModelDetailsService;
    }
}
