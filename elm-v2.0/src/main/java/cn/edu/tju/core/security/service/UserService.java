package cn.edu.tju.core.security.service;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return SecurityUtils.getCurrentUsername().flatMap(userRepository::getUserWithAuthoritiesByUsername);
    }

    public void addUser(User user) {
        userRepository.save(user);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    public User getUserById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.map(EntityUtils::filterEntity).orElse(null);
    }

    public List<User> getUsers() {
        return EntityUtils.filterEntityList(userRepository.findAll());
    }

    public User getUserWithAuthoritiesByUsername(String username) {
        Optional<User> userOptional = userRepository.getUserWithAuthoritiesByUsername(username);
        return userOptional.map(EntityUtils::filterEntity).orElse(null);
    }

    public User getUserWithUsername(String username) {
        Optional<User> userOptional = userRepository.getUserByUsername(username);
        return userOptional.map(EntityUtils::filterEntity).orElse(null);
    }
}
