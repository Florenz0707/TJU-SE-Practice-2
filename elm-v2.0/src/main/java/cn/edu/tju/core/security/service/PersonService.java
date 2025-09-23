package cn.edu.tju.core.security.service;

import cn.edu.tju.core.model.Person;
import cn.edu.tju.core.security.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PersonService {
    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public Person addPerson(Person person) {
        return personRepository.save(person);
    }

    public void updatePerson(Person person) {
        personRepository.save(person);
    }

    public Person getPersonById(Long id) {
        return personRepository.getPersonById(id).orElse(null);
    }

    public Person getPersonByUserName(String username) {
        return personRepository.getPersonByUsername(username).orElse(null);
    }
}
