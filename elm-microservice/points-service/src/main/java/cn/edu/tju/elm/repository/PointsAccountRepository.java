package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.PointsAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
  Optional<PointsAccount> findByUserId(Long userId);
}
