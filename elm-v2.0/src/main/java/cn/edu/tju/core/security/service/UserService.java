package cn.edu.tju.core.security.service;

import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.elm.utils.InternalUserClient;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

  private final InternalUserClient internalUserClient;

  public UserService(InternalUserClient internalUserClient) {
    this.internalUserClient = internalUserClient;
  }

  @Transactional(readOnly = true)
  public Optional<User> getUserWithAuthorities() {
    return SecurityUtils.getCurrentUsername().map(internalUserClient::getUserByUsername);
  }

  public void addUser(User user) {
    internalUserClient.createUser(user);
  }

  public void addPerson(Person person) {
    internalUserClient.createPerson(person);
  }

  public void updateUser(User user) {
    internalUserClient.updateUser(user);
  }

  public User getUserById(Long id) {
    return internalUserClient.getUserById(id);
  }

  public List<User> getUsers() {
    return internalUserClient.getUsers();
  }

  public User getUserWithAuthoritiesByUsername(String username) {
    return internalUserClient.getUserByUsername(username);
  }

  public User getUserWithUsername(String username) {
    return internalUserClient.getUserByUsername(username);
  }
}
