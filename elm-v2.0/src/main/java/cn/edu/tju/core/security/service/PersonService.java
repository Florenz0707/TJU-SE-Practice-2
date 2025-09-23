package cn.edu.tju.core.security.service;

import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.security.repository.PersonRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PersonService {
    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public void addPerson(Person person) {
        personRepository.save(person);
    }

    public void updatePerson(Person person) {
        personRepository.save(person);
    }

    public Person getPersonById(Long id) {
        Optional<Person> personOptional = personRepository.getPersonById(id);
        return personOptional.map(Utils::filterEntity).orElse(null);
    }

    public Person getPersonByUserName(String username) {
        Optional<Person> personOptional = personRepository.getPersonByUsername(username);
        return personOptional.map(Utils::filterEntity).orElse(null);
    }
}
