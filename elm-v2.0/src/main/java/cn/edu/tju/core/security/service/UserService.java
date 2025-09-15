package cn.edu.tju.core.security.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

   private final UserRepository userRepository;

   public UserService(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @Transactional(readOnly = true)
   public Optional<User> getUserWithAuthorities() {
      return SecurityUtils.getCurrentUsername().flatMap(userRepository::findOneWithAuthoritiesByUsername);
   }

   public User addUser(User user) {
      return userRepository.save(user);
   }

   public User updateUser(User user) {
      return userRepository.save(user);
   }

   public Boolean isEmptyUserTable() {
      List<User> userList = userRepository.findAll();
      return userList.isEmpty();
   }
}
