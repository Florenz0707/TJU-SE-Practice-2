package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findOneById(Long id);
}
