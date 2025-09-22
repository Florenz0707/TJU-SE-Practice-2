package cn.edu.tju.elm.repository;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {
    Optional<Business> findOneById(Long id);

    List<Business> findAllByBusinessOwner(User businessOwner);
}
