package cn.edu.tju.elm.repository;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusinessRepository extends JpaRepository<Business, Long> {
    List<Business> findAllByBusinessOwner(User businessOwner);
}
