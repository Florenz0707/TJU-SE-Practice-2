package cn.edu.tju.core.security.repository;

import cn.edu.tju.core.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {
    Optional<Person> getPersonById(Long id);

    Optional<Person> getPersonByUsername(String username);
}
