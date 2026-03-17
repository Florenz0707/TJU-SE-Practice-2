package cn.edu.tju.elm.repository;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.BO.PointsAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
  Optional<PointsAccount> findByUserId(Long userId);

  Optional<PointsAccount> findByUser(User user);
}
